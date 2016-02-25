import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;


public class PanelTemplate extends JPanel {

  /**
   * Create the panel.
   */
  public PanelTemplate() {
    setLayout(new MigLayout("", "[3px,grow,fill][3px,grow,fill]", "[300px,grow,fill]"));

    JScrollPane spWs = new JScrollPane();
    spWs.setVisible(false);
    add(spWs, "cell 0 0,alignx left,growy");

    JButton btnNewButton = new JButton("New button");
    btnNewButton.setVisible(false);
    spWs.setViewportView(btnNewButton);

    JScrollPane spDb = new JScrollPane();
    add(spDb, "cell 1 0,alignx left,growy");

    JButton btnNewButton_1 = new JButton("New button");
    spDb.setViewportView(btnNewButton_1);

  }

}
