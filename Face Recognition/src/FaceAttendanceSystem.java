import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.io.FileOutputStream;


public class FaceAttendanceSystem extends JFrame implements ActionListener {
    private JLabel imageLabel;
    private JButton startButton;
    private VideoCapture camera;
    private CascadeClassifier faceCascade;
    private boolean running;
    private Workbook workbook;
    private Sheet sheet;
    private List<String> attendanceList;


    public FaceAttendanceSystem() {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        faceCascade = new CascadeClassifier("data/haarcascade_frontalface_alt2.xml");


        setTitle("Face Attendance System");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());


        imageLabel = new JLabel();
        add(imageLabel, BorderLayout.CENTER);

        startButton = new JButton("Start Attendance");
        startButton.addActionListener(this);
        add(startButton, BorderLayout.SOUTH);

        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet("Attendance");
        attendanceList = new ArrayList<>();


    }

    public void actionPerformed(ActionEvent e) {
        if (!running) {
            running = true;
            startButton.setText("Stop Attendance");
            startAttendance();

        } else {
            running = false;
            startButton.setText("Start Attendance");
            stopAttendance();

        }
    }

    private void startAttendance() {
        camera = new VideoCapture(0);
        if (!camera.isOpened()) {
            System.out.println("Error opening Camera");
            return;

        }
        new Thread(() -> {
            Mat frame = new Mat();
            while (running) {
                camera.read(frame);
                if (!frame.empty()) {
                    detectAndDisplay(frame);

                }
            }
            camera.release();
            saveAttendanceToExcel();
        }).start();
    }

    private void stopAttendance() {
        running = false;
    }

    private void detectAndDisplay(Mat frame) {
        MatOfRect faces = new MatOfRect();
        Mat grayFrame = new Mat();
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(grayFrame, grayFrame);

        int height = grayFrame.rows();
        int absoluteFaceSize = Math.round(height * 0.2f);
        faceCascade.detectMultiScale(grayFrame, faces, 1.1, 2, 0 | Objdetect.CASCADE_SCALE_IMAGE,
                new Size(absoluteFaceSize, absoluteFaceSize), new Size());


    Rect[] faceArray = faces.toArray();
    for(Rect rect :faceArray) {
        String studentName = recognizeFace(frame.submat(rect));
        if(!attendanceList.contains(studentName)){
            attendanceList.add(studentName);
            Row row = sheet.createRow(attendanceList.size() - 1);
            row.createCell(0).setCellValue(studentName);
            row.createCell(1).setCellValue(LocalDateTime.now().toString());

        }
        Imgproc.rectangle(frame, rect, new Scalar(0, 0, 255), 3);

    }

    ImageIcon imageIcon = new ImageIcon(matToBufferedImage(frame));
    imageLabel.setIcon(imageIcon);
}
private BufferedImage matToBufferedImage(Mat mat) {
    int type = BufferedImage.TYPE_BYTE_GRAY;
    if (mat.channels() > 1) {
        type = BufferedImage.TYPE_3BYTE_BGR;

    }
    int bufferSize = mat.channels() * mat.cols() * mat.rows();
    byte[] b = new byte[bufferSize];
    mat.get(0, 0, b);
    BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
    image.getRaster().setDataElements(0, 0, mat.cols(), mat.rows(), b);
    return image;

}

private String recognizeFace(Mat frame){
        return "Student";
}
private void saveAttendanceToExcel() {
    try (FileOutputStream fileOut = new FileOutputStream("Attendance.xlsx")) {
        workbook.write(fileOut);
        System.out.println("Attendance saved to Excel File");

    } catch (IOException e) {
        e.printStackTrace();
    } finally {
        try {
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

    public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
        FaceAttendanceSystem frame = new FaceAttendanceSystem();
        frame.setVisible(true);
    });
}
}
