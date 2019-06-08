package at.ntrixner.imagedivider;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class ManualMode implements MouseListener, KeyListener {

    private Mat image;
    private Mat image_original;
    private Mat image_scaled;

    private Mat image_out;

    CanvasFrame frame;

    CanvasFrame outFrame;

    OpenCVFrameConverter.ToMat converter;

    Point[] fourGon = new Point[4];

    int i = 0;

    int clamped = -1;

    double scale = 1;

    public static void main(String[] args) throws InterruptedException, IOException {
        ManualMode mode = new ManualMode();
    }

    public ManualMode() {
        image_out = new Mat();
        image_scaled = new Mat();
        image_original = imread("input/input.jpg");
        scale = Math.min(1, Math.min( 1000d / (double)image_original.rows(), 1000d / (double)image_original.cols()));
        resize(image_original, image_scaled, new Size((int)(image_original.cols() * scale), (int)(image_original.rows() * scale)));

        System.out.print(String.format("%d x %d, factor: %f", image_original.rows(), image_original.cols(), scale));


        image = image_scaled.clone();
        frame = new CanvasFrame("Input");
        outFrame = new CanvasFrame("Output");
        converter = new OpenCVFrameConverter.ToMat();

        update();

        frame.getCanvas().addMouseListener(this);
        frame.getCanvas().addKeyListener(this);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void mouseClicked(MouseEvent e) {

    }

    public void mousePressed(MouseEvent e) {
        Point mouse = new Point((int)((double)e.getX()/scale), (int)((double)e.getY()/scale));

        if (this.i == 4 && clamped == -1) {
            int closest = 0;
            for (int i = 1; i < this.i; i++) {
                Point n = fourGon[i];
                if (dist(fourGon[i], mouse) < dist(fourGon[closest], mouse)) {
                    closest = i;
                }
            }
            this.clamped = closest;
            System.out.println(this.clamped);
        } else if (this.i == 4) {

            System.out.println(String.format("x:%d, y:%d", fourGon[clamped].x(), fourGon[clamped].y()));
        }

        update();
    }

    private double dist(Point pointA, Point pointB) {
        return Math.sqrt(Math.pow(pointA.x() - pointB.x(), 2) + Math.pow(pointA.y() - pointB.y(), 2));
    }

    public void mouseReleased(MouseEvent e) {
        Point mouse = new Point((int)((double)e.getX()/scale), (int)((double)e.getY()/scale));
        if (i < 4) {
            System.out.println(String.format("x:%d, y:%d", mouse.x(), mouse.y()));
            fourGon[i] = mouse;
            i++;
        }
        if (clamped != -1) {
            fourGon[clamped] = mouse;
            clamped = -1;
        }
        update();
    }

    public void mouseEntered(MouseEvent e) {

    }

    public void mouseExited(MouseEvent e) {

    }

    public void update() {
        image = image_scaled.clone();
        System.out.println(i + "");
        for (int i = 0; i < this.i; i++) {
            Point pointI = new Point((int)((double)fourGon[i].x() * scale), (int)((double)fourGon[i].y() * scale));
            Point pointIplus1 = new Point((int)((double)fourGon[(i + 1) % this.i].x() * scale), (int)((double)fourGon[(i + 1) % this.i].y() * scale ));

            circle(image, pointI, 3, new Scalar(255d, 0d, 255d, 255d), -1, CV_AA, 0);
            line(image, pointI, pointIplus1, new Scalar(255d, 0d, 255d, 255d), 2, CV_AA, 0);
            //fillConvexPoly(image, fourGon[i], this.i,  new Scalar(255d, 0d, 255d, 120d));
        }
        frame.showImage(converter.convert(image));
        if (i == 4) {
            this.image_out = calcOutput();

            outFrame.showImage(converter.convert(image_out));
        }
    }

    public Mat calcOutput() {
        int distA = (int)(Math.max(dist(fourGon[0], fourGon[1]), dist(fourGon[2], fourGon[3])));
        int distB = (int)(Math.max(dist(fourGon[1], fourGon[2]), dist(fourGon[3], fourGon[0])));

        System.out.println(String.format("%dx%d", distA, distB));

        image_out = new Mat(distB, distA, image_original.type());

        Point2f srcPoints = new Point2f(4);
        srcPoints.position(0).x(fourGon[0].x()).y(fourGon[0].y());
        srcPoints.position(1).x(fourGon[1].x()).y(fourGon[1].y());
        srcPoints.position(2).x(fourGon[2].x()).y(fourGon[2].y());
        srcPoints.position(3).x(fourGon[3].x()).y(fourGon[3].y());

        Point2f dstPoints = new Point2f(4);
        dstPoints.position(0).x(0).y(0);
        dstPoints.position(1).x(0).y(distB);
        dstPoints.position(2).x(distA).y(distB);
        dstPoints.position(3).x(distA).y(0);

        Mat h = getPerspectiveTransform(srcPoints.position(0), dstPoints.position(0));

        System.out.println(h);

        warpPerspective(image_original, image_out, h, image_out.size());


        return image_out;
    }

    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
        if (e.getKeyChar() == 's') {
            try {
                System.out.println("Saving file...");
                Calendar now = Calendar.getInstance();
                DateFormat df = SimpleDateFormat.getDateInstance();
                String name = df.format(now.getTime()) + ".jpg";
                Frame f = converter.convert(image_out);
                Java2DFrameConverter imgConverter = new Java2DFrameConverter();
                BufferedImage img = imgConverter.convert(f);

                File outputFile = new File("output/" + name);
                ImageIO.write(img, "jpg", outputFile);
                System.out.println("Saved file: output/" + name);

                reset();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void keyReleased(KeyEvent e) {
    }

    public void reset(){
        this.fourGon = new Point[4];
        this.i = 0;
        this.clamped = -1;
        update();
    }
}
