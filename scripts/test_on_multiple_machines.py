import paramiko
from multiprocessing import Process
from testcase import Testcase

WORKER_IP_ADDRESSES = [f'2.2.2.{i}' for i in range(103, 103 + 9)]

class TestcaseRunner:
    def __init__(self, testcase: Testcase):
        self.testcase = testcase

    def _run_master(self):
        pass

    def _run_worker(self, worker_index: int):
        ssh = paramiko.SSHClient()
        ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        ssh.connect(WORKER_IP_ADDRESSES[worker_index], username='cyan', key_filename='~/.ssh/id_rsa')
        _, stdout, _ = ssh.exec_command('ls -al')
        print(stdout.read())

    def run(self):
        master_process = Process(target=self._run_master)
        worker_process = Process(target=self._run_worker, args=[0])
        master_process.start()
        worker_process.start()
        master_process.join()
        worker_process.join()

def setup_environment():
    pass
