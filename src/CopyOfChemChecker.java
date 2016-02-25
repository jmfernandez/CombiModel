import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.csic.cnb.data.NetCompound;
import es.csic.cnb.util.Util;


public class CopyOfChemChecker {
  private static final int CONTAINS_DIGIT = 0;
  private static final int CONTAINS_STEROISOMER = 1;

  private Matcher mtLD = Pattern.compile("\\b([LD])\\b").matcher("");
  private Matcher mtNum = Pattern.compile("\\b(\\d+)\\b").matcher("");

  private Map<String, boolean[]> compSynList;
  private Map<String, boolean[]> otherSynList;

  private boolean[] evaluable = {false, false};

  public CopyOfChemChecker() {}

  public CopyOfChemChecker(NetCompound comp, Set<String> synList) {
    boolean[] compEvaluable = {false, false};

    compSynList = new HashMap<String, boolean[]>();
    String compName = (comp.getSbmlName() == null) ? "" : comp.getSbmlName();
    if (containsDigit(compName)) {
      updateSynList(compSynList, compName, CONTAINS_DIGIT);
      compEvaluable[CONTAINS_DIGIT] = true;
    }
    if (containsSteroisomer(compName)) {
      updateSynList(compSynList, compName, CONTAINS_STEROISOMER);
      compEvaluable[CONTAINS_STEROISOMER] = true;
    }

    for (String syn : comp.getSynList()) {
      if (!compSynList.containsKey(syn)) {
        if (containsDigit(syn)) {
          updateSynList(compSynList, syn, CONTAINS_DIGIT);
          compEvaluable[CONTAINS_DIGIT] = true;
        }
        if (containsSteroisomer(syn)) {
          updateSynList(compSynList, syn, CONTAINS_STEROISOMER);
          compEvaluable[CONTAINS_STEROISOMER] = true;
        }
      }
    }

    boolean[] otherEvaluable = {false, false};
    if (compEvaluable[CONTAINS_DIGIT] || compEvaluable[CONTAINS_STEROISOMER]) {
      otherSynList = new HashMap<String, boolean[]>();
      for (String syn : synList) {
        if (containsDigit(syn)) {
          updateSynList(otherSynList, syn, CONTAINS_DIGIT);
          otherEvaluable[CONTAINS_DIGIT] = true;
        }
        if (containsSteroisomer(syn)) {
          updateSynList(otherSynList, syn, CONTAINS_STEROISOMER);
          otherEvaluable[CONTAINS_STEROISOMER] = true;
        }
      }
    }

    evaluable[CONTAINS_DIGIT] = (compEvaluable[CONTAINS_DIGIT] && otherEvaluable[CONTAINS_DIGIT]);
    evaluable[CONTAINS_STEROISOMER] = (compEvaluable[CONTAINS_STEROISOMER] && otherEvaluable[CONTAINS_STEROISOMER]);
  }

  /**
   * @return the evaluable
   */
  public boolean isDigitEvaluable() {
    return evaluable[CONTAINS_DIGIT];
  }

  /**
   * @return the evaluable
   */
  public boolean isSteroisomerEvaluable() {
    return evaluable[CONTAINS_STEROISOMER];
  }

  private boolean containsDigit(String syn) {
    return mtNum.reset(syn).find();
  }

  private boolean containsSteroisomer(String syn) {
    return mtLD.reset(syn).find();
  }

  private void updateSynList(Map<String, boolean[]> list, String syn, int source) {
    boolean[] matches = list.get(syn);
    if (matches == null) {
      matches = new boolean[]{false,false};
    }
    matches[source] = true;
    list.put(syn, matches);
  }

  public void checkDigits() {
    if (evaluable[CONTAINS_DIGIT]) {
      // Recuperar tokens con digitos del compuesto
      for (String csyn : compSynList.keySet()) {
        if (compSynList.get(csyn)[CONTAINS_DIGIT]) {
          // Recuperar tokens con digitos del compuesto
          Map<String, Set<Tk>> ctkList = new HashMap<String,Set<Tk>>();
          String[] ctks = csyn.split("\\W");

          for (int i=0, t=ctks.length; i<t; i++) {
            String ctk = ctks[i];
            if (containsDigit(ctk)) {
              Tk ctoken = new Tk(ctk);
              if (i-1 >= 0)
                ctoken.setpTk(ctks[i-1]);
              if (i+1 < t)
                ctoken.setnTk(ctks[i+1]);

              Set<Tk> list = ctkList.get(ctk);
              if (list == null) {
                list = new HashSet<Tk>();
              }
              list.add(ctoken);
              ctkList.put(ctk, list);
            }
          }

          for (String osyn : otherSynList.keySet()) {
            if (otherSynList.get(osyn)[CONTAINS_DIGIT]) {
              checkDigits(ctkList, osyn);
            }
          }
        }
      }
    }
  }

  public void checkDigits(Map<String, Set<Tk>> ctkList, String osyn) {
    String[] otks = osyn.split("\\W");

    for (int i=0, t=otks.length; i<t; i++) {
      String otk = otks[i];
      if (containsDigit(otk)) {
        Tk otoken = new Tk(otk);
        if (i-1 >= 0)
          otoken.setpTk(otks[i-1]);
        if (i+1 < t)
          otoken.setnTk(otks[i+1]);

        Set<Tk> list = ctkList.get(otk);
        for (Tk ctoken : list) {
//          if(ctoken.pTk.equals(pTk) || ctoken.nTk.equals(nTk)) {
//            match++;
//          }
        }
      }
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("EVALUABLE: ").append(evaluable).append(Util.NEWLINE);
    sb.append("COMP");
    for (String syn : compSynList.keySet()) {
      sb.append("{");
      sb.append(syn).append(": ");
      sb.append(Arrays.toString(compSynList.get(syn)));
      sb.append("}");
    }
    sb.append(Util.NEWLINE);
    sb.append("OTHER");
    for (String syn : otherSynList.keySet()) {
      sb.append("{");
      sb.append(syn).append(": ");
      sb.append(Arrays.toString(otherSynList.get(syn)));
      sb.append("}");
    }

    return sb.toString();
  }


//UDP-N-acetylglucosamine
//UDP-N-acetyl-D-galactosamine(2-)
//  2-Phospho-D-glycerate
//  3-Phosphoglycerate

  /**
   * @param args
   */
  public static void main(String[] args) {
    CopyOfChemChecker cc = new CopyOfChemChecker();

    String s = "3-isopropyl";
    String[] tks = s.split("\\W");
    System.out.println(Arrays.toString(tks));



    Matcher mtNum = Pattern.compile("(\\w?\\w?\\w?.?\\b(\\d+)\\b.?\\w?\\w?\\w?)").matcher("");
    boolean isCompNum = mtNum.reset("3,2-isopropyl").find();
    System.out.println(mtNum.group(1));
    System.out.println(mtNum.group(1).replaceAll("[^,\\w]", ""));
    System.out.println(mtNum.group(2));

//    boolean validateNum = isCompNum;
//    Set<Integer> compNumList = new HashSet<Integer>();
//    while (isCompNum) {
//      compNumList.add(Integer.valueOf(mtNum.group(1)));
//      isCompNum = mtNum.find(mtNum.end(1));
//      System.out.println(mtNum.group(1));
//    }
  }

  class Tk {
    private String tk;
    private String pTk;
    private String nTk;

    public Tk(String tk) {
      this.tk = tk;
      this.pTk = "";
      this.nTk = "";
    }

    public Tk(String tk, String pTk, String nTk) {
      this.tk = tk;
      setpTk(pTk);
      setnTk(nTk);
    }

    /**
     * @return the tk
     */
    public String getTk() {
      return tk;
    }

    /**
     * @param tk the tk to set
     */
    public void setTk(String tk) {
      this.tk = tk;
    }

    /**
     * @return the pTk
     */
    public String getpTk() {
      return pTk;
    }

    /**
     * @param pTk the pTk to set
     */
    public void setpTk(String pTk) {
      if (pTk == null) {
        this.pTk = "";
      }
      else if (pTk.length() >= 3) {
        this.pTk = pTk.substring(0, 3);
      }
      else {
        this.pTk = pTk;
      }
    }

    /**
     * @return the nTk
     */
    public String getnTk() {
      return nTk;
    }

    /**
     * @param nTk the nTk to set
     */
    public void setnTk(String nTk) {
      if (nTk == null) {
        this.nTk = "";
      }
      else if (nTk.length() >= 3) {
        this.nTk = nTk.substring(0, 3);
      }
      else {
        this.nTk = nTk;
      }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((nTk == null) ? 0 : nTk.hashCode());
      result = prime * result + ((pTk == null) ? 0 : pTk.hashCode());
      result = prime * result + ((tk == null) ? 0 : tk.hashCode());
      return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Tk other = (Tk) obj;
      if (!getOuterType().equals(other.getOuterType()))
        return false;
      if (nTk == null) {
        if (other.nTk != null)
          return false;
      } else if (!nTk.equals(other.nTk))
        return false;
      if (pTk == null) {
        if (other.pTk != null)
          return false;
      } else if (!pTk.equals(other.pTk))
        return false;
      if (tk == null) {
        if (other.tk != null)
          return false;
      } else if (!tk.equals(other.tk))
        return false;
      return true;
    }

    private CopyOfChemChecker getOuterType() {
      return CopyOfChemChecker.this;
    }
  }
}
