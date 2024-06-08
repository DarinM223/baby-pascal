package com.d_m.ssa.graphviz;

import com.google.common.io.Files;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class GraphvizViewer {
    public static void viewFile(String title, File file) throws IOException {
        Path pngPath = Paths.get(file.getAbsoluteFile().getParentFile().getAbsolutePath(), Files.getNameWithoutExtension(file.getName()) + ".png");
        File pngFile = pngPath.toFile();
        pngFile.deleteOnExit();
        ProcessBuilder process = new ProcessBuilder(List.of("dot", "-Tpng", file.getAbsolutePath()));
        process.redirectOutput(pngFile);
        try {
            process.start().waitFor();
        } catch (InterruptedException _) {
        }

        CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(title);

            BufferedImage image = null;
            try {
                image = ImageIO.read(pngFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            var imagePanel = new PanImage(image);
            frame.add(imagePanel);
            frame.pack();

            frame.addMouseListener(imagePanel);
            frame.addMouseMotionListener(imagePanel);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    latch.countDown();
                }
            });
            frame.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                        frame.dispose();
                    }
                }
            });
            frame.setVisible(true);
        });
        try {
            latch.await();
        } catch (InterruptedException _) {
        } finally {
            if (!pngFile.delete()) {
                System.out.println("Couldn't delete PNG file");
            }
        }
    }
}
