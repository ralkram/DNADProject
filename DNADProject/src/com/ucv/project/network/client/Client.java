/*
 * 
 */
package com.ucv.project.network.client;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.GregorianCalendar;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.ucv.project.network.RequestType;
import com.ucv.project.tasks.Task;
import com.ucv.project.tasks.Tasks;

/**
 * The Class Client.
 */
public class Client {

  /**
   * The request.
   */
  private static String REQUEST = "Request:";

  /**
   * The payload.
   */
  private static String PAYLOAD = "Payload:";

  /**
   * Client's PC resources.
   */
  private static String PCRESOURCES = "PCResources:";

  /**
   * Client's total number of processors or cores available to the JVM.
   */
  private static long availableProcessors;

  /**
   * Client's total amount of free memory available to the JVM.
   */
  private static long freeMemory;

  /**
   * Client's maximum amount of memory the JVM will attempt to use.
   */
  private static long maxMemory;

  /**
   * Client's total memory currently available to the JVM
   */
  private static long JVMavailableMemory;

  /**
   * The BufferedReader object that makes possible sending messages to the server.
   */
  private BufferedReader in;

  /**
   * The PrinterWrter object that makes possible getting the messages from the server.
   */
  private PrintWriter out;

  /**
   * The GUI frame.
   */
  private JFrame frame = new JFrame("Client");

  /**
   * The message area.
   */
  private JTextArea messageArea = new JTextArea(8, 35);

  /**
   * The show tasks button.
   */
  private JButton showTasksButton = new JButton("Show tasks");

  /**
   * The execute task button.
   */
  private JButton execTaskButton = new JButton("Execute task");

  /**
   * String list consisting of the task picking strategies.
   */
  String[] taskSelectionString = { "Highest complexity", "Lowest complexity", "Oldest" };

  /**
   * The task selection strategy combo box.
   */
  JComboBox<String> taskSelectionComboBox = new JComboBox<String>(taskSelectionString);

  /**
   * The available tasks sent by the server.
   */
  private List<Task> availableTasks;

  /**
   * The name of the task to be executed that is also part of the filename.
   */
  private String task;

  /**
   * Constructor that sets up the GUI and adds ActionListeners for the two available buttons: showTasksButton and
   * execTaskButton. By pressing the "Show tasks" button the client can see all the tasks that his PC can perform. By
   * pressing the "Execute task" button, the client will run one of the presented tasks given the task selection
   * strategy chosen using the bottom right combo box.
   */
  public Client() {
    messageArea.setEditable(false);
    frame.setLayout(new FlowLayout(FlowLayout.LEADING));
    frame.getContentPane().add(new JScrollPane(messageArea));
    frame.getContentPane().add(showTasksButton);
    frame.getContentPane().add(execTaskButton);
    frame.getContentPane().add(taskSelectionComboBox);

    showTasksButton.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        messageArea.setText(null);
        String response = null;
        sendListRequestToServer();
        try {
          response = in.readLine();
          if (response == null || response.isEmpty()) {
            System.exit(0);
          }
        }
        catch (IOException ex) {
          response = "Error: " + ex;
        }
        availableTasks = unmarshallTasks(response).getTasks();
        for (Task task : availableTasks) {
          messageArea.append(task + "\n");
        }
      }
    });

    execTaskButton.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        String response = null;
        int index = taskSelectionComboBox.getSelectedIndex();
        switch (index) {
        case 0:
          task = getMostComplexTask().getCode();
          break;
        case 1:
          task = getLeastComplexTask().getCode();
          break;
        case 2:
          task = getOldestTask().getCode();
          break;
        }
        sendGetTaskRequestFromServer(task);
        try {
          response = in.readLine();
          String responseMessage = response.split(" ")[0];
          RequestType requestAvailable = RequestType.valueOf(responseMessage.split(":")[1]);
          if (requestAvailable.equals(RequestType.UNAVAILABLE)) {
            messageArea.append("Task Unavailable" + "\n");
            return;
          }
          PrintWriter writer = new PrintWriter(task + "2exec.java", "UTF-8");
          writer.println(response.split("Request:REQUEST_TASK")[1]);
          writer.close();
        }
        catch (IOException e1) {
          e1.printStackTrace();
        }
        executeTask();

      }
    });

  }

  /**
   * Transforms xml to {@link com.ucv.project.tasks.Tasks}
   * 
   * @param xmlTasksResponse the object
   * @return the Tasks object
   */
  private Tasks unmarshallTasks(String xmlTasksResponse) {
    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(Tasks.class);

      Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
      StringReader xmlReader = new StringReader(xmlTasksResponse);
      return (Tasks) jaxbUnmarshaller.unmarshal(xmlReader);
    }
    catch (JAXBException je) {
      je.printStackTrace();
    }
    return null;
  }

  /**
   * Determines the task with the highest complexity value from the availableTasks list.
   * 
   * @return The task with the highest complexity value.
   */
  private Task getMostComplexTask() {
    Task mostComplexTask = new Task();

    for (Task task : availableTasks) {
      double taskComplexity = task.getComplexity();

      if (taskComplexity > mostComplexTask.getComplexity()) {
        mostComplexTask = task;
      }
    }

    return mostComplexTask;
  }

  /**
   * Determines the task with the lowest complexity value from the availableTasks list.
   * 
   * @return The task with the lowest complexity value.
   */
  private Task getLeastComplexTask() {
    Task leastComplexTask = new Task();
    leastComplexTask.setComplexity(999);

    for (Task task : availableTasks) {
      double taskComplexity = task.getComplexity();

      if (taskComplexity < leastComplexTask.getComplexity()) {
        leastComplexTask = task;
      }
    }
    return leastComplexTask;
  }

  /**
   * Determines the task with the oldest creation date from the availableTasks list.
   * 
   * @return The task with the oldest creation date.
   */
  private Task getOldestTask() {
    Task oldestTask = new Task();
    oldestTask.setDate(new GregorianCalendar());

    for (Task task : availableTasks) {
      GregorianCalendar taskDate = task.getDate();
      if (taskDate.before(oldestTask.getDate())) {
        oldestTask = task;
      }
    }

    return oldestTask;
  }

  /**
   * Sends task list request to server.
   */
  private void sendListRequestToServer() {
    out.println(REQUEST + RequestType.REQUEST_LIST_OF_TASKS + ":" + PCRESOURCES + " Available processors "
        + availableProcessors + " Free memory " + freeMemory + " Max memory " + maxMemory + " JVM available memory "
        + JVMavailableMemory);
  }

  /**
   * Sends a server request containing the task id, letting the server know which task to send back.
   * 
   * @param taskCode The task's code or id.
   */
  private void sendGetTaskRequestFromServer(String taskCode) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(REQUEST + RequestType.REQUEST_TASK + " ");
    stringBuilder.append(PAYLOAD + taskCode);

    out.println(stringBuilder);
  }

  /**
   * Compiles and executes the task given by the server. The task received from the server will be a .java file.
   */
  public void executeTask() {
    try {
      String taskToExcute = task + "2exec";
      Process p = Runtime.getRuntime().exec("javac " + taskToExcute + ".java");
      p.waitFor();
      p = Runtime.getRuntime().exec("java " + taskToExcute);
      p.waitFor();
      BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line = reader.readLine();
      messageArea.append("Result:" + line + "\n");

      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append(REQUEST + RequestType.FINISHED + " ");
      stringBuilder.append(PAYLOAD + "Result[" + task + "]" + line);

      out.println(stringBuilder);
    }
    catch (Exception e) {
      e.getMessage();
    }
  }

  /**
   * Gets the client's PC resources: available processors(cores), free memory, maximum memory, JVM available memory.
   */
  public static void getPCResources() {
    availableProcessors = Runtime.getRuntime().availableProcessors();
    freeMemory = Runtime.getRuntime().freeMemory();
    maxMemory = Runtime.getRuntime().maxMemory();
    JVMavailableMemory = Runtime.getRuntime().totalMemory();
  }

  /**
   * Method that makes the connection to the server by creating a socket and instantiating the PrinterWriter and the
   * BufferedReader objects.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public void connectToServer() throws IOException {

    @SuppressWarnings("resource")
    Socket socket = new Socket("localhost", 9898);
    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    out = new PrintWriter(socket.getOutputStream(), true);

  }

  /**
   * The main method.
   *
   * @param args The arguments.
   * @throws Exception The exception.
   */
  public static void main(String[] args) throws Exception {
    Client client = new Client();
    client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    client.frame.setSize(415, 230);
    client.frame.setVisible(true);
    client.connectToServer();

    getPCResources();

  }
}
