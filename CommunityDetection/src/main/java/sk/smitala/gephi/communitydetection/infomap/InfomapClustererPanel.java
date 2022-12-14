/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.smitala.gephi.communitydetection.infomap;

/**
 *
 * @author smitalm
 */
public class InfomapClustererPanel extends javax.swing.JPanel {

    /**
     * Creates new form InfomapClustererPanel
     */
    public InfomapClustererPanel() {
	initComponents();
    }

    public void setTeleport(double teleport) {
	this.teleport.setText(teleport + "");
    }

    public double getTeleport() {
	try {
	    return Double.parseDouble(this.teleport.getText());
	} catch (NumberFormatException e) {
	    e.printStackTrace(System.err);
	    return InfomapClusterer.TELEPORT_CHANCE_DEFAULT;
	}
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
        teleportLabel = new javax.swing.JLabel();
        teleport = new javax.swing.JTextField();
        teleportDescription = new javax.swing.JLabel();

        header.setDescription(org.openide.util.NbBundle.getMessage(InfomapClustererPanel.class, "InfomapClustererPanel.header.description")); // NOI18N
        header.setTitle(org.openide.util.NbBundle.getMessage(InfomapClustererPanel.class, "InfomapClustererPanel.header.title")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(teleportLabel, org.openide.util.NbBundle.getMessage(InfomapClustererPanel.class, "InfomapClustererPanel.teleportLabel.text")); // NOI18N

        teleport.setText(org.openide.util.NbBundle.getMessage(InfomapClustererPanel.class, "InfomapClustererPanel.teleport.text")); // NOI18N

        teleportDescription.setForeground(new java.awt.Color(102, 102, 102));
        org.openide.awt.Mnemonics.setLocalizedText(teleportDescription, org.openide.util.NbBundle.getMessage(InfomapClustererPanel.class, "InfomapClustererPanel.teleportDescription.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(header, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addGap(36, 36, 36)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(teleportDescription))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(teleportLabel)
                        .addGap(18, 18, 18)
                        .addComponent(teleport, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(175, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(header, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(44, 44, 44)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(teleportLabel)
                    .addComponent(teleport, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(teleportDescription)
                .addGap(0, 26, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.jdesktop.swingx.JXHeader header;
    private javax.swing.JTextField teleport;
    private javax.swing.JLabel teleportDescription;
    private javax.swing.JLabel teleportLabel;
    // End of variables declaration//GEN-END:variables
}
