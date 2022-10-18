/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package sk.smitala.gephi.communitydetection.markovalgo;

import org.openide.util.Exceptions;
import sk.smitala.gephi.communitydetection.utils.CommonUtils;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URI;
import java.net.URISyntaxException;

public class MarkovAlgoClustererPanel extends javax.swing.JPanel {

    /**
     * Creates new form MarkovAlgoClustererPanel
     */
    public MarkovAlgoClustererPanel() {
        initComponents();
        setLinkButton();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(700, 433);
    }

    private void setLinkButton() {
        final URI uri;
        try {
            uri = new URI("https://snap.stanford.edu/class/cs224w-2013/projects2013/cs224w-065-final.pdf");
        } catch (URISyntaxException ex) {
            Exceptions.printStackTrace(ex);
            linkButton.setEnabled(false);
            return;
        }
        linkButton.setToolTipText(uri.toString());
        linkButton.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                CommonUtils.open(uri);
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
    }

    public int getKMeansN(){
        return Integer.parseInt(KMeansNField.getText());
    }




    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        header = new org.jdesktop.swingx.JXHeader();
        linkButton = new javax.swing.JLabel();
        KMeansNLabel = new javax.swing.JLabel();
        KMeansNField = new javax.swing.JTextField();
        ParamsLabel = new javax.swing.JLabel();

        header.setDescription(org.openide.util.NbBundle.getMessage(MarkovAlgoClustererPanel.class, "MarkovAlgoClustererPanel.header.description_1")); // NOI18N
        header.setTitle(org.openide.util.NbBundle.getMessage(MarkovAlgoClustererPanel.class, "MarkovAlgoClustererPanel.header.title")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(linkButton, org.openide.util.NbBundle.getMessage(MarkovAlgoClustererPanel.class, "MarkovAlgoClustererPanel.linkButton.text")); // NOI18N
        linkButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        org.openide.awt.Mnemonics.setLocalizedText(KMeansNLabel, org.openide.util.NbBundle.getMessage(MarkovAlgoClustererPanel.class, "MarkovAlgoClustererPanel.KMeansNLabel.text_1")); // NOI18N

        KMeansNField.setText(org.openide.util.NbBundle.getMessage(MarkovAlgoClustererPanel.class, "MarkovAlgoClustererPanel.KMeansNField.text_1")); // NOI18N

        ParamsLabel.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(ParamsLabel, org.openide.util.NbBundle.getMessage(MarkovAlgoClustererPanel.class, "MarkovAlgoClustererPanel.ParamsLabel.text_1")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(header, javax.swing.GroupLayout.PREFERRED_SIZE, 518, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(linkButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addGap(10, 10, 10)
                            .addComponent(KMeansNLabel)
                            .addGap(18, 18, 18)
                            .addComponent(KMeansNField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(ParamsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 368, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addContainerGap(156, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(header, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(linkButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(117, Short.MAX_VALUE))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(203, 203, 203)
                    .addComponent(ParamsLabel)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(KMeansNLabel)
                        .addComponent(KMeansNField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addContainerGap(55, Short.MAX_VALUE)))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField KMeansNField;
    private javax.swing.JLabel KMeansNLabel;
    private javax.swing.JLabel ParamsLabel;
    private org.jdesktop.swingx.JXHeader header;
    private javax.swing.JLabel linkButton;
    // End of variables declaration//GEN-END:variables
}