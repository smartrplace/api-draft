package org.smartrplace.util.file;

 import java.math.BigDecimal;
import java.math.BigInteger;

import org.apache.commons.io.FileUtils;
	     
/** Note that packages as documented here are useful:
 * http://commons.apache.org/proper/commons-io/javadocs/api-release/index.html?org/apache/commons/io/package-summary.html
 * @author dnestle
 *
 */
public class ApacheFileAdditions {
    /**
     * The number of bytes in a kilobyte.
     *
     * @since 2.4
     */
     public static final BigInteger ONE_KB_BI = BigInteger.valueOf(FileUtils.ONE_KB);     /**
    
    * The number of bytes in a megabyte.
     *
     * @since 2.4
     */
    public static final BigInteger ONE_MB_BI = ONE_KB_BI.multiply(ONE_KB_BI);
    /**
     * The number of bytes in a gigabyte.
     *
     * @since 2.4
     */
    public static final BigInteger ONE_GB_BI = ONE_KB_BI.multiply(ONE_MB_BI);
    public static final BigInteger ONE_TB_BI = ONE_KB_BI.multiply(ONE_GB_BI);
    public static final long ONE_PB = FileUtils.ONE_KB * FileUtils.ONE_TB;
    public static final BigInteger ONE_PB_BI = ONE_KB_BI.multiply(ONE_TB_BI);
    public static final long ONE_EB = FileUtils.ONE_KB * ONE_PB;
    public static final BigInteger ONE_EB_BI = ONE_KB_BI.multiply(ONE_PB_BI);
    public static final BigInteger ONE_ZB = BigInteger.valueOf(FileUtils.ONE_KB).multiply(BigInteger.valueOf(ONE_EB));
    public static final BigInteger ONE_YB = ONE_KB_BI.multiply(ONE_ZB);
	 /* 
	      * @param size
	      *            the number of bytes
	      * @return a human-readable display value (includes units - YB, ZB, EB, PB, TB, GB, MB, KB or bytes)
	      * @see <a href="https://issues.apache.org/jira/browse/IO-226">IO-226 - should the rounding be changed?</a>
	      * @since 2.4
	      */

	 public static String byteCountToDisplaySize(BigInteger size) {
         String displaySize;
 
         final BigDecimal sizeBD = new BigDecimal(size);
         if (size.divide(FileUtils.ONE_YB).compareTo(BigInteger.ZERO) > 0) {
             displaySize = getThreeSigFigs(sizeBD.divide(new BigDecimal(FileUtils.ONE_YB))) + " YB";
         } else if (size.divide(FileUtils.ONE_ZB).compareTo(BigInteger.ZERO) > 0) {
             displaySize = getThreeSigFigs(sizeBD.divide(new BigDecimal(FileUtils.ONE_ZB))) + " ZB";
         } else if (size.divide(ONE_EB_BI).compareTo(BigInteger.ZERO) > 0) {
             displaySize = getThreeSigFigs(sizeBD.divide(new BigDecimal(ONE_EB_BI))) + " EB";
         } else if (size.divide(ONE_PB_BI).compareTo(BigInteger.ZERO) > 0) {
             displaySize = getThreeSigFigs(sizeBD.divide(new BigDecimal(ONE_PB_BI))) + " PB";
         } else if (size.divide(ONE_TB_BI).compareTo(BigInteger.ZERO) > 0) {
             displaySize = getThreeSigFigs(sizeBD.divide(new BigDecimal(ONE_TB_BI))) + " TB";
         } else if (size.divide(ONE_GB_BI).compareTo(BigInteger.ZERO) > 0) {
             displaySize = getThreeSigFigs(sizeBD.divide(new BigDecimal(ONE_GB_BI))) + " GB";
         } else if (size.divide(ONE_MB_BI).compareTo(BigInteger.ZERO) > 0) {
             displaySize = getThreeSigFigs(sizeBD.divide(new BigDecimal(ONE_MB_BI))) + " MB";
         } else if (size.divide(ONE_KB_BI).compareTo(BigInteger.ZERO) > 0) {
             displaySize = getThreeSigFigs(sizeBD.divide(new BigDecimal(ONE_KB_BI))) + " KB";
         } else {
             displaySize = String.valueOf(size) + " bytes";
         }
         return displaySize;
     }
 
     private static String getThreeSigFigs(BigDecimal size) {
         String number = size.toString();
         StringBuffer trimmedNumber = new StringBuffer();
         int cnt = 0;
         boolean hasDecimal = false;
         for (final char digit : number.toCharArray()) {
             if (cnt < 3 || !hasDecimal) {
                 trimmedNumber.append(digit);
             }
             if (digit == '.') {
                 hasDecimal = true;
             } else {
                 cnt++;
             }
         }
         String displaySize = trimmedNumber.toString();
         if (hasDecimal) {
             while (displaySize.endsWith("0")) {
                 displaySize = displaySize.substring(0, displaySize.length() - 1);
             }
             if (displaySize.endsWith(".")) {
                 displaySize = displaySize.substring(0, displaySize.length() - 1);
             }
         }
         return displaySize;
     }

}
