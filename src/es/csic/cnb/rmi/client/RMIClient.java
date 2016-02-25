package es.csic.cnb.rmi.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.EmptyBorder;

import net.miginfocom.swing.MigLayout;
import es.csic.cnb.rmi.ProgressData;
import es.csic.cnb.rmi.RMIInterfaz;
import es.csic.cnb.rmi.client.gui.ManualSelectionDialog;
import es.csic.cnb.util.Util;

public class RMIClient extends JFrame {
  private static final long serialVersionUID = 2067865863868838435L;

  private static Registry registry;

  private ManualSelectionDialog dialog;

  private JPanel contentPane;

  private RMIInterfaz rmi;
  private JButton btnRun;
  private JButton btnExit;
  private JProgressBar pbar;
  private JLabel lbInf;

  private SwingWorker<Void, Void> worker;

  private JLabel lbTotal;
  private JLabel lbTotalCur;
  private JLabel lbTotalPend;

  /**
   * Create the frame.
   */
  public RMIClient() {
    try {
      for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
        if ("Nimbus".equals(info.getName())) {
          UIManager.setLookAndFeel(info.getClassName());
          break;
        }
      }
    } catch (Exception e) {
      // If Nimbus is not available, you can set the GUI to another look and feel.
    }

    setTitle("Manual Curation: " + Util.RMI_HOST + ":" + Util.RMI_PORT);
    setResizable(false);

    try {
      registry = LocateRegistry.getRegistry(Util.RMI_HOST, Util.RMI_PORT);
      rmi = (RMIInterfaz) registry.lookup(RMIInterfaz.class.getSimpleName());

      initComponents();

      worker = new SwingWorker<Void, Void>() {
        Timer timer;
        @Override
        protected Void doInBackground() throws Exception {
          timer = new Timer (1000, new ActionListener () {
            public void actionPerformed(ActionEvent e) {
              try {
                ProgressData pdata = rmi.getProgressFromServer();
                pbar.setValue(pdata.getPercentaje());
                lbInf.setText(pdata.getInf());

                lbTotal.setText(String.valueOf(rmi.getTotal()));
                lbTotalPend.setText(lbTotal.getText());

              } catch (RemoteException e1) {
                e1.printStackTrace();
              }
            }
          });
          timer.start();

          rmi.loadData();

          return null;
        }
        @Override
        public void done() {
          timer.stop();

          pbar.setValue(100);
          lbInf.setText("Load completed");

          try {
            lbTotal.setText(String.valueOf(rmi.getTotal()));
            lbTotalPend.setText(lbTotal.getText());
          } catch (RemoteException e1) {
            e1.printStackTrace();
          }
        }
      };
      worker.execute();

    } catch (RemoteException e) {
      e.printStackTrace();
    } catch (NotBoundException e) {
      e.printStackTrace();
    }
  }

  private void initComponents() throws RemoteException {
    dialog = new ManualSelectionDialog(rmi);

    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    setSize(450, 300);
    setLocationRelativeTo(null);

    contentPane = new JPanel();
    contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
    setContentPane(contentPane);
    contentPane.setLayout(new BorderLayout(10, 5));

    JPanel panelButton = new JPanel();
    FlowLayout fl_panelButton = (FlowLayout) panelButton.getLayout();
    fl_panelButton.setAlignment(FlowLayout.RIGHT);
    contentPane.add(panelButton, BorderLayout.SOUTH);

    btnRun = new JButton("Run Manual Curation");
    btnRun.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent arg0) {
        dialog.display();
        try {
          lbTotalPend.setText(String.valueOf(rmi.getTotal()));
          StringBuilder sb = new StringBuilder();
          sb.append(rmi.getTotalManual()).append(" man. + ");
          sb.append(rmi.getTotalAuto()).append(" auto.");
          lbTotalCur.setText(sb.toString());
        } catch (RemoteException e) {
          e.printStackTrace();
        }
      }
    });
    panelButton.add(btnRun);

    btnExit = new JButton("Exit");
    btnExit.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        close();
      }
    });
    panelButton.add(btnExit);

    JPanel panelProgress = new JPanel();
    contentPane.add(panelProgress, BorderLayout.NORTH);
    panelProgress.setLayout(new MigLayout("", "[312.00px,grow,fill]", "[14px][]"));

    pbar = new JProgressBar();
    pbar.setEnabled(false);
    pbar.setMinimumSize(new Dimension(200, 10));
    pbar.setPreferredSize(new Dimension(148, 10));
    panelProgress.add(pbar, "cell 0 0,alignx left,aligny center");

    lbInf = new JLabel("");
    lbInf.setEnabled(false);
    lbInf.setFont(new Font("Dialog", Font.PLAIN, 11));
    panelProgress.add(lbInf, "cell 0 1,aligny top");

    JPanel panel = new JPanel();
    contentPane.add(panel, BorderLayout.WEST);
    panel.setLayout(new MigLayout("", "[][]", "[][][]"));

    JLabel lblNewLabel = new JLabel("Total compounds loaded:");
    panel.add(lblNewLabel, "cell 0 0");
    lbTotal = new JLabel("0");
    lbTotal.setFont(new Font("Dialog", Font.BOLD, 12));
    panel.add(lbTotal, "cell 1 0");

    JLabel lblNewLabel_1 = new JLabel("Curated compounds:");
    panel.add(lblNewLabel_1, "cell 0 1,alignx left,aligny baseline");
    lbTotalCur = new JLabel("0");
    lbTotalCur.setForeground(Color.BLUE);
    lbTotalCur.setFont(new Font("Dialog", Font.PLAIN, 12));
    panel.add(lbTotalCur, "cell 1 1");

    JLabel lblNewLabel_2 = new JLabel("Remaining compounds:");
    panel.add(lblNewLabel_2, "cell 0 2");
    lbTotalPend = new JLabel("0");
    lbTotalPend.setForeground(Color.RED);
    lbTotalPend.setFont(new Font("Dialog", Font.PLAIN, 12));
    panel.add(lbTotalPend, "cell 1 2,aligny baseline");

    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        super.windowClosing(e);
        close();
      }
    });
  }

  private void close() {
    int response = JOptionPane.showConfirmDialog(this,
            "Salir del proceso de curacion manual." +
                    "\n¿Continuar?",
                    "Informacion",
                    JOptionPane.YES_NO_OPTION);
    // Continuar proceso de cancelacion
    if (response == JOptionPane.YES_OPTION) {
      worker.cancel(true);

      dialog.dispose();
      dispose();

      try {
        rmi.close();
      } catch (RemoteException e1) {
        e1.printStackTrace();
      }

      System.exit(0);
    }
  }


  /**
   * Launch the application.
   */
  public static void main(String[] args) {
    EventQueue.invokeLater(new Runnable() {
      public void run() {
        try {
          RMIClient frame = new RMIClient();
          frame.setVisible(true);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }
}
