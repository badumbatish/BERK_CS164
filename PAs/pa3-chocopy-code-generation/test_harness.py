import argparse
import difflib
import multiprocessing
import os
import platform
import subprocess
import sys
import time


jar_opt = ":"
if platform.system() == 'Windows':
    jar_opt = ";"


def complement_substr(main_string, sub_str):
    if sub_str in main_string:
        # Find the index of the substring
        index = main_string.find(sub_str)
        # Create a new string by concatenating the parts before and after the substring
        return main_string[:index] + main_string[index + len(sub_str):]
    else:
        return main_string  # If substring doesn't exist, return the original string


def diff_handler(output1, output2, quiet=False):
    diff_out = difflib.unified_diff(output1[0], output2[0], fromfile='Student implementation stdout',
                                    tofile='Staff implementation  stdout', lineterm='')
    # diff_err = difflib.unified_diff(output1[1], output2[1], fromfile='Student implementation stderr',
    #                                 tofile='Staff implementation stderr', lineterm='')

    if quiet:
        count = 0
        for _ in diff_out:
            count += 1

        return count + len(output1[1]) != 0

    count = 0
    for line in diff_out:
        sys.stdout.write(line + '\n')
        count += 1

    # if we get nothing from count, and we are also getting something from stderr
    # We better mark the test as failing
    if len(output1[1]) != 0 or len(output2[1]) != 0:
        print("Student stderr")
        for line in output1[1]:
            sys.stdout.write(line + '\n')

        print("Staff stderr")
        for line in output2[1]:
            sys.stdout.write(line + '\n')

    return count + len(output1[1]) != 0


class TestHarness:
    @staticmethod
    def run_command(command, is_strip=True, timeout=4):
        if "input.py" in command:
            with open("src/test/data/pa3/sample/input.py.in", 'r') as f:
                proc = subprocess.Popen(command, stdin=f, stdout=subprocess.PIPE,
                                        stderr=subprocess.PIPE,
                                        text=True,
                                        shell=True,
                                        preexec_fn=os.setsid)
        else:
            proc = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                    text=True,
                                    shell=True, preexec_fn=os.setsid)

        try:
            result_stdout, result_stderr = proc.communicate(timeout=timeout)
        except subprocess.TimeoutExpired:
            import signal
            os.killpg(os.getpgid(proc.pid), signal.SIGTERM)  # Send the signal to all the process groups
            hang_msg = "\nCommand {command} hangs for {timeout} seconds"

            proc.communicate()
            result_stdout, result_stderr = hang_msg, hang_msg
            # result_stdout += hang_msg
            # result_stderr += hang_msg

        if is_strip:
            return result_stdout.strip().splitlines(), result_stderr.strip().splitlines()
        else:
            return result_stdout, result_stderr

    def compare_and_contrast(self, command1, command2, is_strip=True, quiet=False):
        if not quiet:
            sys.stdout.write("=============================================\n")
            sys.stdout.write("---Begin running with student implementation\n")
            sys.stdout.write("---Finish running with student implementation\n")
            output1 = self.run_command(command1, is_strip)
            sys.stdout.write("=============================================\n")

            sys.stdout.write("---Begin running with staff implementation\n")
            output2 = self.run_command(command2, is_strip)
            sys.stdout.write("---Finish running with staff implementation\n")
            sys.stdout.write("=============================================\n")

            sys.stdout.write("               \n")
            sys.stdout.write("               \n")

            sys.stdout.write(f'Command 1: {command1}\n')
            sys.stdout.write(f'Command 2: {command2}\n')
            sys.stdout.write("=============================================\n")
        else:
            output1 = self.run_command(command1, is_strip)
            output2 = self.run_command(command2, is_strip)

        count = diff_handler(output1, output2, quiet)

        filename = os.path.basename(command1.split()[-1])
        base_path = self.true_directory[len(self.base_path):]
        if count == 0:
            sys.stdout.write(f"Test of {base_path}/{filename} passed \n")
        else:
            sys.stdout.write(f"Test of {base_path}/{filename} failed \n")

        return count == 0

    def set_dir(self, args_dir):
        self.true_directory = f'{self.base_path}/{args_dir}'

        if "bench" in args_dir:
            self.mapping_function = self.benchmark
        else:
            self.mapping_function = self.test_file

    def __init__(self, parser_args):
        # Only store non-optional arguments
        self.args = parser_args

        # Maven command to run on every test iteration.
        self.mvn_command = "mvn -T 7 package"
        self.quiet = (args.quiet == "yes")
        self.mapping_function = self.test_file
        self.base_path = 'src/test/data/pa3/'
        self.set_dir(args.dir)

        # Default command
        self.true_command = f'java -cp "chocopy-ref.jar{jar_opt}target/assignment.jar" chocopy.ChocoPy'
        self.reports = []

    def command_build(self):
        timeout = 15
        proc = subprocess.Popen(self.mvn_command, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                text=True,
                                shell=True, preexec_fn=os.setsid, )
        try:
            result_stdout, result_stderr = proc.communicate(timeout=timeout)
        except subprocess.TimeoutExpired:
            import signal
            os.killpg(os.getpgid(proc.pid), signal.SIGTERM)  # Send the signal to all the process groups
            hang_msg = f'\nCommand {self.mvn_command} hangs for {timeout} seconds'
            print(hang_msg)
            proc.communicate()
            sys.exit()

        if proc.returncode != 0:
            print(f"Build command `{self.mvn_command}` failed.")
            print("Printing the log now")
            for line in result_stdout.strip().splitlines():
                print(line)
            for line in result_stderr.strip().splitlines():
                print(line)

            print(f"Please fix the build error of `{self.mvn_command}` and rerun the test harness")
            sys.exit()

    def handle_test_dir(self, folder):
        self.set_dir(folder)
        sample_status = self.test_all()

        report = f"REPORT - `{folder}`: {sample_status}"
        self.reports.append(report)
        return report

    # test_single(): accepts a file (not necessarily python) to be run with true_command.
    # Calls via test_all,
    def test_file(self, python_file):
        true_file = f'{self.true_directory}/{python_file}'
        command1 = f'{self.true_command} --pass=rrs --run {true_file}'
        command2 = f'{self.true_command} --pass=rrr --run {true_file}'
        result = self.compare_and_contrast(command1, command2, True, self.quiet)

        return result

    def benchmark(self, python_file):
        iteration = 10
        true_file = f'{self.true_directory}/{python_file}'
        command1 = f'{self.true_command} --pass=rrs {true_file}'
        command2 = f'{self.true_command} --pass=rrr {true_file}'

        tic = time.time()
        for i in range(0, iteration):
            line_file1 = len(self.run_command(command1)[0])
        toc = time.time()

        time1 = (toc - tic)/iteration

        tic = time.time()
        for i in range(0, iteration):
            line_file2 = len(self.run_command(command2)[0])
        toc = time.time()

        time2 = (toc - tic)/iteration

        line_percentage = (line_file1 / (line_file2+0.1)) * 100
        time_percentage = (time1 / time2) * 100
        result = self.test_file(python_file)


        RED = "\033[31m"
        GREEN = "\033[32m"
        BLUE = "\033[34m"
        RESET = "\033[0m"
        if (line_percentage > 130):
            line_status = RED
        elif line_percentage <=80 :
            line_status = BLUE
        else:
            line_status = GREEN

        if (time_percentage > 130):
            time_status = RED
        elif time_percentage <=80 :
            time_status = BLUE
        else:
            time_status = GREEN

        print(f"--For `{python_file}`, student asm is {line_status}{line_percentage:.3f}{RESET} relative to staff asm")
        print(f"--For `{python_file}`, student run time is {time_status}{time_percentage:.3f}{RESET} relative to staff run time")
        return result

    def test_all(self):

        all_files = os.listdir(self.true_directory)
        python_files = [file for file in all_files if file.endswith('.py')]
        with multiprocessing.Pool(min(len(python_files), 8)) as pool:
            results = pool.imap(self.mapping_function, python_files)
            count = sum(results)
        return (f"{count} out of {len(python_files)} tests passed, {len(python_files) - count} out "
                f"of {len(python_files)} failed")

    def handle_checkpoint(self):
        count = 0
        sample_checkpoint_files = ["literal_bool.py", "op_cmp_int.py", "literal_int.py", "op_div_mod.py",
                                   "literal_str.py", "op_logical.py", "id_global.py", "op_mul.py", "id_local.py",
                                   "op_negate.py", "var_assign.py", "op_sub.py", "call.py", "stmt_if.py",
                                   "call_with_args.py", "stmt_while.py", "nested.py", "stmt_return_early.py",
                                   "nested2.py", "op_add.py",
                                   "op_cmp_bool.py"]

        benchmark_checkpoint_files = ["prime.py", "exp.py"]
        sys.stdout.write(f"Sample checkpoint: {sample_checkpoint_files}\n")
        sys.stdout.write(f"Benchmark checkpoint: {benchmark_checkpoint_files}\n")

        with multiprocessing.Pool(8) as pool:
            self.set_dir("sample")
            results = pool.imap(self.mapping_function, sample_checkpoint_files)
            #         results = map(self.test_file, sample_checkpoint_files)
            count = count + sum(results)

        with multiprocessing.Pool(2) as pool:
            self.set_dir("benchmarks")
            results = pool.imap(self.mapping_function, benchmark_checkpoint_files)
            #         results = map(self.test_file, benchmark_checkpoint_files)
            count = count + sum(results)

        len_cp_files = len(sample_checkpoint_files) + len(benchmark_checkpoint_files)
        report = (f"{count} out of {len_cp_files} tests passed, {len_cp_files - count} out "
                  f"of {len_cp_files} failed")

        return report

    def handle_args(self):
        if '-h' in args or '--help' in args:
            sys.exit()
        else:
            if args.command == "test-single":
                if args.file is None:
                    print("Should pass in a file when running test-single")
                    sys.exit()
                print("Test single")
                self.command_build()

                # Directly setting self.quiet to False, we need as much info as we can
                self.quiet = False
                self.mapping_function(args.file)

            elif args.command == "test-all":
                print(f"Test all in {args.dir}")
                self.command_build()
                if args.dir == '.':
                    self.handle_test_recursive_dir("benchmarks")
                    self.handle_test_recursive_dir("sample")
                    self.handle_test_recursive_dir("student")
                else:
                    self.handle_test_recursive_dir(args.dir)

                for report in self.reports:
                    print(report)

            elif args.command == "test-checkpoint":
                print(f"Test checkpoints sample and benchmark")
                self.command_build()

                report = self.handle_checkpoint()
                print(report)

            elif args.command == "asm-out-ref":
                true_file = f'{self.true_directory}/{self.args.file}'
                # run_command(self.mvn_command)
                if self.true_directory in ['sample', 'benchmarks']:
                    command1 = f'{self.true_command} --pass=..r {true_file}.ast.typed'
                else:
                    self.command_build()
                    command1 = f'{self.true_command} --pass=rrr {true_file}'
                print(command1)
                asm_output = self.run_command(command1, False)
                print(asm_output[0])
                print(asm_output[1])

            elif args.command == "asm-out-student":
                true_file = f'{self.true_directory}/{self.args.file}'
                self.command_build()

                command1 = f'{self.true_command} --pass=rrs {true_file}'
                print(command1)
                asm_output = self.run_command(command1, False)
                print(asm_output[0])
                print(asm_output[1])

    # TODO: Add a recursive function test-recursive-dir() where
    # Base case is when there is no more directories, then we loop through all the tests
    # If there is more directory, loop over those directories with the test-recursive-dir()
    def handle_test_recursive_dir(self, folder):
        self.set_dir(folder)

        old_folder = folder
        entries = os.listdir(self.true_directory)
        for entry in entries:
            if os.path.isdir(os.path.join(self.true_directory, entry)):
                print(f'{folder}/{entry}')
                self.handle_test_dir(f'{folder}/{entry}')
            self.set_dir(folder)

        print(f'----TEST IN FOLDER: {old_folder}----')
        return self.handle_test_dir(old_folder)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(prog='Test Harness', description="Python CS164 Test harness script")
    parser.add_argument("-c", "--command",
                        choices=["test-single", "test-all", "test-checkpoint", "asm-out-ref", "asm-out-student"],
                        required=True,
                        help="Select which commands the test harness should run.")
    parser.add_argument("-d", "--dir", required=False, default="sample",
                        help="Default is `sample`. Provide a directory, for command to work in, starting in "
                             "test/data/pa3/. both `sample` and"
                             "`benchmark` will be run with"
                             " Jasmine's impl of test_harness. Selecting `.` runs both"
                        )
    parser.add_argument("-f", "--file",
                        help="Provide a file in a directory specified by dir, based on {command} to "
                             "run the executable on. "
                             "Ignored if command is *-all. Errors if file is not inputted with test-single")
    parser.add_argument("-q", "--quiet", choices=["yes", "no", ], required=False, default="yes",
                        help="Default is `yes`. Options for test_harness to either be quiet about comparison or not. "
                             "No will output extra. With test-single, this will"
                             " always be verbose")

    args = parser.parse_args()

    test_harness = TestHarness(args)
    test_harness.handle_args()
