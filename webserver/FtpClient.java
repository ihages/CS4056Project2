package webserver;

import java.io.*;
import java.net.*;
import java.util.regex.*;

/**
 *
 * This is the main FTP client. Study the code and figure out what 
 * each function does before adding to it. You only need to add code 
 * wherever you see a '?'
 *
 * @author Giovani
 * 
 */
public class FtpClient {

    final static String CRLF = "\r\n";
    private boolean DEBUG = false;		// Debug Flag
    private Socket controlSocket = null;
    private BufferedReader controlReader = null;
    private DataOutputStream controlWriter = null;
    private String currentResponse;

    /*
     * Constructor
     */
    public FtpClient() {
    }

    /*
     * Connect to the FTP server
     * @param username: the username you use to login to your FTP session
     * @param password: the password associated with the username
     */
    public void connect(String username, String password) {
        try {
            // establish the control socket
            controlSocket = new Socket("localhost", 6789);

            // get references to the socket input and output streams
            controlReader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
            controlWriter = new DataOutputStream(controlSocket.getOutputStream());

            // check if the initial connection response code is OK
            if (checkResponse(220)) { //220 = service ready for new user
                System.out.println("Succesfully connected to FTP server");
            }

            // send user name and password to ftp server
            sendCommand("USER " + username, 331); //331 = user name okay, need password
            sendCommand("PASS " + password, 230); //230 = user logged in, proceed

        } catch (UnknownHostException e) {
            System.out.println("UnknownHostException: " + e);
        } catch (IOException e) {
            System.out.println("IOException: " + e);
        }
    }

    /*
     * Retrieve the file from FTP server after connection is established
     * @param file_name: the name of the file to retrieve
     */
    public void getFile(String file_name) {
	int data_port = 0; // initialize the data port        
	try {
            // change to current (root) directory first
            sendCommand("CWD /", 250); //250 = requested file action okay, completed

            // set to passive mode and retrieve the data port number from response
            currentResponse = sendCommand("PASV", 227); //227 = entering passive mode
            data_port = extractDataPort(currentResponse);

            // connect to the data port 
            Socket data_socket = new Socket(controlSocket.getInetAddress(), data_port);
            DataInputStream data_reader = new DataInputStream(data_socket.getInputStream());

            // download file from ftp server
            sendCommand("RETR " + file_name, 150); // 150 = file status okay; about to open data connection

            // check if the transfer was successful
            if (checkResponse(226)) { //226 = closing data connection, requested file action successful
                System.out.println("File transfer completed successfully.");
            }

            // Write data on a local file
            createLocalFile(data_reader, file_name);
            data_socket.close();

        } catch (UnknownHostException e) {
            System.out.println("UnknownHostException: " + e);
        } catch (IOException e) {
            System.out.println("IOException: " + e);
        }
    }

    /*
     * Close the FTP connection
     */
    public void disconnect() {
        try {
            controlReader.close();
            controlWriter.close();
            controlSocket.close();
        } catch (IOException e) {
            System.out.println("IOException: " + e);
        }
    }

    /*
     * Send ftp command 
     * @param command: the full command line to send to the ftp server
     * @param expected_code: the expected response code from the ftp server
     * @return the response line from the ftp server after sending the command
     */
    private String sendCommand(String command, int expected_response_code) {
        String response = "";
        try {
            // send command to the ftp server
            controlWriter.writeBytes(command);

            // get response from ftp server
            response = controlReader.readLine();
            if (DEBUG) {
                System.out.println("Current FTP response: " + response);
            }

            // check validity of response  
            if (!response.startsWith(String.valueOf(expected_response_code))) {
                throw new IOException(
                        "Bad response: " + response);
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e);
        }
        return response;
    }

    /*
     * Check the validity of the ftp response, the response code should
     * correspond to the expected response code
     * @param expected_code: the expected ftp response code
     * @return response status: true if successful code
     */
    private boolean checkResponse(int expected_code) {
        boolean response_status = true;
        try {
            currentResponse = controlReader.readLine();
            if (DEBUG) {
                System.out.println("Current FTP response: " + currentResponse);
            }
            if (!currentResponse.startsWith(String.valueOf(expected_code))) {
                response_status = false;
                throw new IOException(
                        "Bad response: " + currentResponse);
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e);
        }
        return response_status;
    }

    /*
     * Given the complete ftp response line of setting data transmission mode
     * to passive, extract the port to be used for data transfer
     * @param response_line: the ftp response line
     * @return the data port number 
     */
    private int extractDataPort(String response_line) {
        int data_port = 0;
        Pattern pattern = Pattern.compile("\\((.*?)\\)");
        Matcher matcher = pattern.matcher(response_line);
        String[] str = new String[6];
        if (matcher.find()) {
            str = matcher.group(1).split(",");
        }
        if (DEBUG) {
            System.out.println("Port integers: " + str[4] + "," + str[5]);
        }
        data_port = Integer.valueOf(str[4]) * 256 + Integer.valueOf(str[5]);
        if (DEBUG) {
            System.out.println("Data Port: " + data_port);
        }
        return data_port;
    }

    /*
     * Create the file locally after retreiving data over the FTP data stream.
     * @param dis: the data input stream 
     * @param file_name: the name of the file to create 
     */
    private void createLocalFile(DataInputStream dis, String file_name) {
        byte[] buffer = new byte[1024];
        int bytes = 0;
        try {
            FileOutputStream fos = new FileOutputStream(new File(file_name));
            while ((bytes = dis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytes);
            }
            dis.close();
            fos.close();
        } catch (FileNotFoundException e) {
            System.out.println("FileNotFoundException" + e);
        } catch (IOException e){
            System.out.println("IOException: " + e);
        } 
    }
}
