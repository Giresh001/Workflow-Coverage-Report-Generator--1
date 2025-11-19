package com.report;

import com.jcraft.jsch.*;

import java.util.Vector;

public class FileTransfer {

    /*
    * ssh PC_NAME@IP
    * password: pwd
    * C:\Users\320249581 -> local
    * C:\Users\Desktop\temp.txt
     */
    static String LOCAL_FILE_PATH = null;
    static String REMOTE_FILE_PATH = null;
    static String REMOTE_PC_IP = null;
    static String USER_NAME = null;
    static String PASSWORD = null;
    static int PORT = 22;

    public static void main(String[] args) {
        System.out.println("*********************************");
        System.out.println("enter details");
        LOCAL_FILE_PATH =  args[0];
        REMOTE_FILE_PATH = args[1];
        REMOTE_PC_IP = args[2];
        USER_NAME = args[3];
        PASSWORD = args[4];

        System.out.println(
               "Local file path : " + LOCAL_FILE_PATH
                + "rmeote file path : " + REMOTE_FILE_PATH
                + "remote IP : " + REMOTE_PC_IP
                + "username" +USER_NAME + "password" + PASSWORD
        );

        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(USER_NAME, REMOTE_PC_IP, PORT);
            session.setPassword(PASSWORD);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftpChannel = (ChannelSftp) channel;
            // Get - pull file from remote server
            // PUt - send file from remote server

            sftpChannel.get(REMOTE_FILE_PATH, LOCAL_FILE_PATH);

            sftpChannel.exit();
            session.disconnect();

            System.out.println("File copied successfully.");
        } catch (JSchException | SftpException e) {
            System.out.println("File not found");
            e.printStackTrace();
        }
    }
}

