package com.d_m.ssa.graphviz;

import com.google.common.io.Files;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GraphvizViewer {
    public static void viewFile(File file) throws IOException {
        Path pngFile = Paths.get(file.getAbsoluteFile().getParentFile().getAbsolutePath(), Files.getNameWithoutExtension(file.getName()) + ".png");
        Process process = Runtime.getRuntime().exec(String.format("dot -Tpng %s > %s", file.getAbsoluteFile(), pngFile.toString()));
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            System.out.println("Process interrupted");
        }

        JFrame frame = new JFrame();
        ImageIcon icon = new ImageIcon(pngFile.toString());
        frame.add(new JLabel(icon));
        frame.setSize(1000, 1000);
        frame.pack();
        frame.setVisible(true);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
