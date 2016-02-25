package es.csic.cnb.rmi.server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import es.csic.cnb.curation.BeanCompound;
import es.csic.cnb.curation.ManualCurationManager;
import es.csic.cnb.data.WSCompound;
import es.csic.cnb.rmi.ProgressData;
import es.csic.cnb.rmi.RMIInterfaz;
import es.csic.cnb.ws.ChebiException;

public class RMIInterfazImpl extends UnicastRemoteObject implements RMIInterfaz {
  private static final long serialVersionUID = 4975261827248710123L;

  private ManualCurationManager manMgr;

  protected RMIInterfazImpl() throws RemoteException {
    super();
    manMgr = new ManualCurationManager();
  }

  @Override
  public ProgressData getProgressFromServer() throws RemoteException {
    return manMgr.getProgressData();
  }

  @Override
  public void loadData() throws RemoteException, InterruptedException {
    manMgr.loadManualCurationData();
  }

  @Override
  public int getTotal() throws RemoteException {
    return manMgr.getTotal();
  }

  @Override
  public int getTotalManual() throws RemoteException {
    return manMgr.getTotalManual();
  }

  @Override
  public int getTotalAuto() throws RemoteException {
    return manMgr.getTotalAuto();
  }

  @Override
  public void close() throws RemoteException {
    manMgr.close();
  }

  @Override
  public BeanCompound getNextCompound() throws RemoteException, InterruptedException, ChebiException {
    return manMgr.getNextCompound();
  }

  @Override
  public void saveBothCandidates(String cleanId, WSCompound wsCandidate, WSCompound dbCandidate)
          throws RemoteException {
    manMgr.saveBothCandidates(cleanId, wsCandidate, dbCandidate);
  }

  @Override
  public void saveWsCandidate(String cleanId, WSCompound wsCandidate) throws RemoteException {
    manMgr.saveWsCandidate(cleanId, wsCandidate);
  }

  @Override
  public void saveDbCandidate(String cleanId, WSCompound dbCandidate) throws RemoteException {
    manMgr.saveDbCandidate(cleanId, dbCandidate);
  }

  @Override
  public void saveNoneCandidate(String cleanId) throws RemoteException {
    manMgr.saveNoneCandidate(cleanId);
  }

  @Override
  public void skipCompound(String cleanId) throws RemoteException {
    manMgr.skipCompound(cleanId);
  }

  @Override
  public void exit(String cleanId) throws RemoteException {
    manMgr.exit(cleanId);
  }
}
