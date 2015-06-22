/*
 * 
 */
package com.ucv.project.network.server;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.ucv.project.network.RequestType;
import com.ucv.project.tasks.Task;
import com.ucv.project.tasks.Tasks;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

/**
 * The Class Server.
 */
public class Server {

  /**
   * The unresolved tasks.
   */
  private static Tasks unresolvedTasks = new Tasks();
  /**
   * The sum task.
   */
  private static String SUM_TASK = "Sum";

  /**
   * The pow task.
   */
  private static String POW_TASK = "Pow";

  /**
   * The sub task.
   */
  private static String SUB_TASK = "Sub";

  /**
   * The Inner Class ConnectionThread.
   */
  private static class ConnectionThread extends Thread {

    /**
     * The connection socket that assures the communication with the client.
     */
    private Socket socket;

    /**
     * The connected client's number.
     */
    private int clientNumber;

    /**
     * The BufferedReader object that makes possible sending messages to the client.
     */
    private BufferedReader in;

    /**
     * The PrinterWrter object that makes possible getting the messages from the client.
     */
    private PrintWriter out;

    /**
     * The lock object used to synchronize the class.
     */
    private static Object lock = new Object();

    /**
     * The ConnectionThread constructor
     *
     * @param socket - The connection socket that assures the communication with the client.
     * @param clientNumber - The connected client's number.
     */
    public ConnectionThread(Socket socket, int clientNumber) {
      this.socket = socket;
      this.clientNumber = clientNumber;
      log("New connection with client# " + clientNumber + " at " + socket);
    }

    /*
     * (non-Javadoc) Overriden run method that instantiates the BufferedReader and PrintWriter objects and is
     * permanently listening for messages from clients. Depending on the requests received, the list of available tasks
     * or a the first one from the list is sent.
     */
    @SuppressWarnings("incomplete-switch")
    public void run() {
      try {
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        while (true) {
          String input = in.readLine();
          if (input == null || input.equals(".")) {
            break;
          }
          String[] clientMessage = input.split(" ");
          String requestHeader = clientMessage[0].split(":")[1];
          System.out.println(requestHeader);
          switch (RequestType.valueOf(requestHeader)) {

          case REQUEST_LIST_OF_TASKS:
            Tasks availableTasks = new Tasks();
            for (Task task : unresolvedTasks.getTasks()) {
              if (task.getEstimatedProcessorsNeeded() <= Long.parseLong(clientMessage[3])
                  && task.getEstimatedMemoryNeeded() <= Long.parseLong(clientMessage[13])) {
                availableTasks.getTasks().add(task);
              }
            }
            StringBuilder listStrings = new StringBuilder();
            listStrings.append(marshalTaskAsXML(availableTasks));
            out.println(listStrings);
            break;

          case REQUEST_TASK:
            String requestedTask = clientMessage[1].split(":")[1];
            sendTaskToClient(requestedTask);
            break;

          case FINISHED:
            String resultAfterExecution = clientMessage[1].split(":")[1];
            log(resultAfterExecution);
            break;

          }
        }
      }
      catch (IOException e) {
        log("Error handling client# " + clientNumber + ": " + e);
      }
      catch (InterruptedException e) {
        e.printStackTrace();
      }
      finally {
        try {
          socket.close();
        }
        catch (IOException e) {
          log("Couldn't close a socket.");
        }
        log("Connection with client# " + clientNumber + " closed");
      }
    }

    /**
     * Transforms {@link com.ucv.project.tasks.Tasks} to xml format.
     */
    private String marshalTaskAsXML(Tasks tasks) {
      try {
        JAXBContext jaxbContext = JAXBContext.newInstance(Tasks.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        StringWriter sw = new StringWriter();

        jaxbMarshaller.marshal(tasks, sw);
        return sw.toString();

      }
      catch (JAXBException je) {
        je.printStackTrace();
      }
      return null;
    }

    /**
     * Method that prints a message to the console.
     *
     * @param message The message.
     */
    private void log(String message) {
      System.out.println(message);
    }

    /**
     * Method that send a java file to the client in a StringBuilder
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws InterruptedException the interrupted exception
     */
    public void sendTaskToClient(String requestedTask) throws IOException, InterruptedException {
      List<Task> tasks = unresolvedTasks.getTasks();

      synchronized (lock) {
        for (Iterator<Task> iterator = tasks.iterator(); iterator.hasNext();) {
          Task task = iterator.next();

          if (task.getCode().equals(requestedTask)) {
            iterator.remove();
            log(requestedTask);

            BufferedReader br = new BufferedReader(new FileReader(new File(requestedTask + ".java")));
            String line;

            StringBuilder javaTask = new StringBuilder();
            javaTask.append("Request:" + RequestType.REQUEST_TASK + " ");
            while ((line = br.readLine()) != null) {
              javaTask.append(line);
            }
            out.println(javaTask);
            br.close();
          }

        }
      }
    }
  }

  /**
   * The main method.
   *
   * @param args The arguments.
   * @throws Exception The exception.
   */
  public static void main(String[] args) throws Exception {
    System.out.println("The server is running.");
    int clientNumber = 0;
    ServerSocket listener = new ServerSocket(9898);

    unresolvedTasks.getTasks().add(new Task("Sum", 305, new GregorianCalendar(2015, 1, 13, 20, 0), 2, 10000));
    unresolvedTasks.getTasks().add(new Task("Pow", 160, new GregorianCalendar(2015, 3, 10, 10, 0), 9, 20000));
    unresolvedTasks.getTasks().add(new Task("Sub", 200, new GregorianCalendar(2014, 1, 13, 20, 0), 2, 10000));
    unresolvedTasks.getTasks().add(new Task("Add", 276, new GregorianCalendar(2012, 3, 10, 10, 0), 1, 20000));
    unresolvedTasks.getTasks().add(new Task("Do", 426, new GregorianCalendar(2011, 1, 13, 20, 0), 10, 10000));
    unresolvedTasks.getTasks().add(new Task("Ret", 123, new GregorianCalendar(2013, 3, 10, 10, 0), 1, 20000));
    try {
      while (true) {
        new ConnectionThread(listener.accept(), clientNumber++).start();
      }
    }
    finally {
      listener.close();
    }
  }
}