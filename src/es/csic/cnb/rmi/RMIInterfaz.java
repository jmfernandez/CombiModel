package es.csic.cnb.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import es.csic.cnb.curation.BeanCompound;
import es.csic.cnb.data.WSCompound;
import es.csic.cnb.ws.ChebiException;

public interface RMIInterfaz extends Remote {

  /**
   * @return BeanCompound el compuesto a curar.
   * @throws RemoteException
   */
  public BeanCompound getNextCompound() throws RemoteException, InterruptedException, ChebiException;

  /**
   * Guarda candidatos WS y DB en la base de datos
   * @param cleanId
   * @param wsCandidate
   * @param dbCandidate
   * @throws RemoteException
   */
  public void saveBothCandidates(String cleanId, WSCompound wsCandidate, WSCompound dbCandidate) throws RemoteException;

  /**
   * Guarda candidato WS en la base de datos
   * @param cleanId
   * @param wsCandidate
   * @throws RemoteException
   */
  public void saveWsCandidate(String cleanId, WSCompound wsCandidate) throws RemoteException;

  /**
   * Guarda candidato DB en la base de datos
   * @param cleanId
   * @param dbCandidate
   * @throws RemoteException
   */
  public void saveDbCandidate(String cleanId, WSCompound dbCandidate) throws RemoteException;

  /**
   * Guarda el compuesto en la base de datos ya que no se ha seleccionado candidato
   * @param cleanId
   * @throws RemoteException
   */
  public void saveNoneCandidate(String cleanId) throws RemoteException;

  /**
   * Se salta este compuesto.
   * @param cleanId
   * @throws RemoteException
   */
  public void skipCompound(String cleanId) throws RemoteException;

  /**
   * Salir de la curacion manual.
   */
  public void exit(String cleanId) throws RemoteException;

  /**
   * @return Numero de elementos de la cola.
   * @throws RemoteException
   */
  public int getTotal() throws RemoteException;

  /**
   * @return Numero de elementos curados manualmente.
   * @throws RemoteException
   */
  public int getTotalManual() throws RemoteException;

  /**
   * @return Numero de elementos curados automaticamente.
   * @throws RemoteException
   */
  public int getTotalAuto() throws RemoteException;

  public ProgressData getProgressFromServer() throws RemoteException;

  public void loadData() throws RemoteException, InterruptedException;

  public void close() throws RemoteException;
}
