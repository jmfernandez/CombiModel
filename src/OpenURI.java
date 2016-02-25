import java.awt.Desktop;
import java.net.URI;

public class OpenURI {

  public void openBrowser(String url) {
    if (Desktop.isDesktopSupported()) {
      Desktop desktop = Desktop.getDesktop();
      if (desktop.isSupported(Desktop.Action.BROWSE)) {
        try {
          desktop.browse( new URI(url) );
        }
        catch (Exception e) {
          System.err.println(e.getMessage());
        }
      }
      else {
        System.err.println("Desktop doesn't support the browse action (fatal)");
      }
    }
    else {
      System.err.println("Desktop is not supported (fatal)");
    }
  }

  public static void main(String [] args) {
    String as = "http://johnbokma.com/mexit/2008/08/19/java-open-url-default-browser.html";
    
  }
}