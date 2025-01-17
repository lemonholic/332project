import paramiko
from paramiko import SSHClient
from scp import SCPClient

USER = 'cyan'
KEYFILE = '/home/cyan/.ssh/id_rsa'

def exec_command_blocking(ssh: SSHClient, command: str):
    _, stdout, stderr = ssh.exec_command(command)
    return stdout.read().decode(), stderr.read().decode()

def createSSHClient(server, port):
    ssh = SSHClient()
    ssh.load_system_host_keys()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(server, port, username=USER, key_filename=KEYFILE)
    return ssh

def getFile(client, remote_path, local_path):
    scp = SCPClient(client.get_transport())
    scp.get(remote_path, local_path)

def putFile(client, file, remote_path):
    scp = SCPClient(client.get_transport())
    scp.put(file, remote_path)
