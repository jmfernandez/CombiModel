import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.csic.cnb.data.NetCompound;
import es.csic.cnb.util.Util;


public class ChemChecker {
  private static final int CONTAINS_DIGIT = 0;
  private static final int CONTAINS_STEROISOMER = 1;

  private Matcher mtLD = Pattern.compile("\\b([LD])\\b").matcher("");
  private Matcher mtNum = Pattern.compile("\\b(\\d+)\\b").matcher("");
  private Matcher mtNumCtx = Pattern.compile("(\\w?\\w?\\w?.?\\b(\\d+)\\b.?\\w?\\w?\\w?)").matcher("");

  private Map<String, boolean[]> compSynList;
  private Map<String, boolean[]> otherSynList;

  private boolean[] evaluable = {false, false};

  public ChemChecker() {}

  public ChemChecker(NetCompound comp, Set<String> synList) {
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


          for (String osyn : otherSynList.keySet()) {
            if (otherSynList.get(osyn)[CONTAINS_DIGIT]) {


            }
          }
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
    ChemChecker cc = new ChemChecker();

    String s = "3-isopropyl";
    String[] tks = s.split("\\W");
    System.out.println(Arrays.toString(tks));



    Matcher mtNum = Pattern.compile("([a-z]?[a-z]?[a-z]?.?\\b(\\d+)\\b.?[a-z]?[a-z]?[a-z]?)").matcher("");
    boolean isCompNum = mtNum.reset("--3,2-isopropyl").find();
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
}
