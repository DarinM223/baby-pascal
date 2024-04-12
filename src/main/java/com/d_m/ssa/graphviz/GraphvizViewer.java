package com.d_m.ssa.graphviz;

import com.google.common.io.Files;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class GraphvizViewer {
    public static void viewFile(File file) throws IOException {
        Path pngPath = Paths.get(file.getAbsoluteFile().getParentFile().getAbsolutePath(), Files.getNameWithoutExtension(file.getName()) + ".png");
        File pngFile = pngPath.toFile();
        pngFile.deleteOnExit();
        ProcessBuilder process = new ProcessBuilder(List.of("dot", "-Tpng", file.getAbsolutePath()));
        process.redirectOutput(pngFile);
        process.start();

        CountDownLatch latch = new CountDownLatch(1);
        JFrame frame = new JFrame();
        ImageIcon icon = new ImageIcon(pngFile.toString());
        frame.add(new JLabel(icon));
        frame.setSize(1000, 1000);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException _) {
        }
    }
}
