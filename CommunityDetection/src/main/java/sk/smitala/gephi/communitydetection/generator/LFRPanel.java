/*
Copyright 2008-2010 Gephi
Authors : Mathieu Bastian <mathieu.bastian@gephi.org>
Website : http://www.gephi.org

This file is part of Gephi.

DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

Copyright 2011 Gephi Consortium. All rights reserved.

The contents of this file are subject to the terms of either the GNU
General Public License Version 3 only ("GPL") or the Common
Development and Distribution License("CDDL") (collectively, the
"License"). You may not use this file except in compliance with the
License. You can obtain a copy of the License at
http://gephi.org/about/legal/license-notice/
or /cddl-1.0.txt and /gpl-3.0.txt. See the License for the
specific language governing permissions and limitations under the
License.  When distributing the software, include this License Header
Notice in each file and include the License files at
/cddl-1.0.txt and /gpl-3.0.txt. If applicable, add the following below the
License Header, with the fields enclosed by brackets [] replaced by
your own identifying information:
"Portions Copyrighted [year] [name of copyright owner]"

If you wish your version of this file to be governed by only the CDDL
or only the GPL Version 3, indicate your decision by adding
"[Contributor] elects to include this software in this distribution
under the [CDDL or GPL Version 3] license." If you do not indicate a
single choice of license, a recipient has the option to distribute
your version of this file under either the CDDL, the GPL Version 3 or
to extend the choice of license to its licensees as provided above.
However, if you add GPL Version 3 code and therefore, elected the GPL
Version 3 license, then the option applies only if the new code is
made subject to such option by the copyright holder.

Contributor(s):

Portions Copyrighted 2011 Gephi Consortium.
*/

package sk.smitala.gephi.communitydetection.generator;

import java.awt.Dimension;

/**
 *
 * @author Mathieu Bastian
 */
public class LFRPanel extends javax.swing.JPanel {

    /** Creates new form LFRPanel */
    public LFRPanel() {
        initComponents();
	
	nodes.setPreferredSize(new Dimension(50, 20));
	degree.setPreferredSize(new Dimension(50, 20));
	degreeMin.setPreferredSize(new Dimension(50, 20));
	degreeExp.setPreferredSize(new Dimension(50, 20));
	commExp.setPreferredSize(new Dimension(50, 20));
	mixingParam.setPreferredSize(new Dimension(50, 20));
//	degree.setText(String.valueOf(lfrGenerator.getAverageDegree()));
//	degreeMin.setText(String.valueOf(lfrGenerator.getMinimumDegree()));
//	degreeExp.setText(String.valueOf(lfrGenerator.getNodeDegreeExponent()));
//	commExp.setText(String.valueOf(lfrGenerator.getCommunitySizeExponent()));
//	mixingParam.setText(String.valueOf(lfrGenerator.getMixingParameter()));
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        nodes = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        degree = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        commExp = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        degreeExp = new javax.swing.JTextField();
        mixingParam = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        degreeMin = new javax.swing.JTextField();

        jLabel1.setText(org.openide.util.NbBundle.getMessage(LFRPanel.class, "LFRPanel.jLabel1.text")); // NOI18N

        nodes.setText(org.openide.util.NbBundle.getMessage(LFRPanel.class, "LFRPanel.nodes.text")); // NOI18N

        jLabel2.setText(org.openide.util.NbBundle.getMessage(LFRPanel.class, "LFRPanel.jLabel2.text")); // NOI18N

        degree.setText(org.openide.util.NbBundle.getMessage(LFRPanel.class, "LFRPanel.degree.text")); // NOI18N

        jLabel3.setText(org.openide.util.NbBundle.getMessage(LFRPanel.class, "LFRPanel.jLabel3.text")); // NOI18N

        commExp.setText(org.openide.util.NbBundle.getMessage(LFRPanel.class, "LFRPanel.commExp.text")); // NOI18N

        jLabel4.setText(org.openide.util.NbBundle.getMessage(LFRPanel.class, "LFRPanel.jLabel4.text")); // NOI18N

        degreeExp.setText(org.openide.util.NbBundle.getMessage(LFRPanel.class, "LFRPanel.degreeExp.text")); // NOI18N

        mixingParam.setText(org.openide.util.NbBundle.getMessage(LFRPanel.class, "LFRPanel.mixingParam.text")); // NOI18N

        jLabel5.setText(org.openide.util.NbBundle.getMessage(LFRPanel.class, "LFRPanel.jLabel5.text")); // NOI18N

        jLabel6.setForeground(new java.awt.Color(102, 102, 102));
        jLabel6.setText(org.openide.util.NbBundle.getMessage(LFRPanel.class, "LFRPanel.jLabel6.text")); // NOI18N

        jLabel7.setForeground(new java.awt.Color(102, 102, 102));
        jLabel7.setText(org.openide.util.NbBundle.getMessage(LFRPanel.class, "LFRPanel.jLabel7.text")); // NOI18N

        jLabel8.setForeground(new java.awt.Color(102, 102, 102));
        jLabel8.setText(org.openide.util.NbBundle.getMessage(LFRPanel.class, "LFRPanel.jLabel8.text")); // NOI18N

        jLabel9.setText(org.openide.util.NbBundle.getMessage(LFRPanel.class, "LFRPanel.jLabel9.text")); // NOI18N

        degreeMin.setText(org.openide.util.NbBundle.getMessage(LFRPanel.class, "LFRPanel.degreeMin.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel9)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 37, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(degreeMin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(degree, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(nodes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(commExp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(degreeExp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(mixingParam, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel7)
                            .addComponent(jLabel6)
                            .addComponent(jLabel8))))
                .addGap(27, 27, 27))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(nodes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(degree, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(degreeMin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(degreeExp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(commExp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(mixingParam, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addContainerGap(20, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    protected javax.swing.JTextField commExp;
    protected javax.swing.JTextField degree;
    protected javax.swing.JTextField degreeExp;
    protected javax.swing.JTextField degreeMin;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    protected javax.swing.JTextField mixingParam;
    protected javax.swing.JTextField nodes;
    // End of variables declaration//GEN-END:variables
}