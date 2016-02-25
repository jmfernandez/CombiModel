package es.csic.cnb.ws;

public class ChebiException extends Exception {
  private static final long serialVersionUID = 1L;

  public enum Field {NAME, FM, ID};

  public ChebiException() {
    super();
  }

  public ChebiException(Throwable e) {
    super(e);
  }

  public ChebiException(Field field, String value) {
    super("Problem with " + field + ": " + value);
  }

  public ChebiException(Field field, String value, Throwable e) {
    super("Problem with " + field + ": " + value, e);
  }
}
