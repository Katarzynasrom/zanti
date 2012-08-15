package com.zimperium.zanti.cracker;

import java.io.File;

import android.os.Environment;

public class CrackerCore {

	public final static int MAX_TASKS = 16;
	
	public final static String[] crack_protocols = new String[]{"cisco", "cisco-enable", "cvs", "firebird", "ftp", "ftps", "http-get /", "http-head /", "https-get /", "https-head /",
		"http-post-form /", "http-get-form /", "https-post-form /", "https-get-form /", "icq", "imap", "irc", "ldap2", "ldap3-crammd5", "ldap3-digestmd5", "mssql", "mysql",
		"nntp", "oracle-listener", "oracle-sid", "pcanywhere", "pcnfs", "pop3", "rdp", "rexec", "rlogin", "rsh", "sip", "smb", "smtp", "smtp-enum", "snmp", "socks5", "ssh",
		"svn", "teamspeak", "telnet", "vmauthd", "vnc", "xmpp"};

	public static String Port2Protocol(int port) {

		switch (port) {
			case 22 :
				return "ssh";
			case 23 :
				return "telnet";
			case 25 :
				return "smtp";
			case 80 :
				return "http-get /";
			case 110 :
				return "pop3";
			case 119 :
				return "nntp";
			case 143 :
				return "imap";
			case 194 :
				return "irc";
			case 220 :
				return "imap";
			case 443 :
				return "https-get /";
			case 445 :
				return "smb";
			case 2401 :
				return "cvs";
			case 3050 :
				return "firebird";
			case 3306 :
				return "mysql";
			case 3690 :
				return "svn";
			case 5222 :
				return "xmpp";
			case 5269 :
				return "xmpp";
			case 5432 :
				return "postgres";
			case 8010 :
				return "xmpp";
			case 8080 :
				return "http-get /";
			default :
				//return "http-get /";
				return null;
		}
	}

	public static String GetPasswordsFileForPort(Integer port, int selected_crack_type, String custom_pass_file) {

		// web.pass = huge
		// ssh.pass = big
		// windows.pass = small

		switch (selected_crack_type) {
			case 0 :
				switch (port) {
					case 22 :
						return "ssh.pass";
					case 23 :
						return "ssh.pass";
					case 80 :
						return "web.pass";
					case 443 :
						return "ssh.pass";
					case 445 :
						return "windows.pass";
					case 3389 :
						return "windows.pass";
					case 5432 :
						return "mssql.pass";
				}
				break;
			case 1 :
				return "optimized.pass";
			case 2 :
				return "ssh.pass";
			case 3 :
				return "web.pass";
			case 4 :
				if (custom_pass_file != null)
					return custom_pass_file;
				final String filename = Environment.getExternalStorageDirectory().getPath() + "/anti/custom.pass";
				if (new File(filename).exists())
					return filename;
				break;
		}
		return "ssh.pass";
	}

	public static String GetUsersFileForPort(Integer port, int selected_crack_type) {
		switch (selected_crack_type) {
			case 0 :
			case 1 :
			case 2 :
			case 3 :
				switch (port) {
					case 22 :
						return "ssh.user";
					case 23 :
						return "ssh.user";
					case 80 :
						return "web.user";
					case 443 :
						return "web.user";
					case 445 :
						return "windows.user";
					case 3389 :
						return "windows.user";
					case 5432 :
						return "mssql.user";
					case 8080 :
						return "web.user";
				}
				return "web.user";
			case 4 :
				final String filename = Environment.getExternalStorageDirectory().getPath() + "/anti/custom.user";
				if (new File(filename).exists())
					return filename;
				return "web.user";
		}
		return "ssh.user";
	}

	public static String generateHydraCommand(int selected_crack_type, String charset, int port, String Target, String protocol_name, String custom_pass_file) {
		if (selected_crack_type == 5) {
			String users_lst = CrackerCore.GetUsersFileForPort(port, selected_crack_type);
			// String username =
			// crack_incremental_username.getText().toString();
			if (port < 0) {
				return String.format("(./hydra -L %s -x \"%s\" -t %d -f -w 10 %s %s 2>&1 ; echo DONE)&\n", users_lst, charset, MAX_TASKS, Target, protocol_name);
			} else {
				return String.format("(./hydra -L %s -x \"%s\" -t %d -f -w 10 -s %d %s %s 2>&1 ; echo DONE)&\n", users_lst, charset, MAX_TASKS, port, Target, protocol_name);
			}
		} else {
			String users_lst = CrackerCore.GetUsersFileForPort(port, selected_crack_type);
			String pass_lst = CrackerCore.GetPasswordsFileForPort(port, selected_crack_type, custom_pass_file);
			if (port < 0) {
				return String.format("(./hydra -L %s -P %s -t %d -f -w 10 %s %s 2>&1  ; echo DONE)&\n", users_lst, pass_lst, MAX_TASKS, Target, protocol_name);
			} else {
				return String.format("(./hydra -L %s -P %s -t %d -f -w 10 -s %d %s %s 2>&1  ; echo DONE)&\n", users_lst, pass_lst, MAX_TASKS, port, Target, protocol_name);
			}
		}
	}
}
