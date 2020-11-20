package au.org.amc.seedim.action;

public class CreatePowerUserReportDAO {

  
  private String siteShortname;
  private String siteTitle;
  private String powerUsers;
  
  
  public CreatePowerUserReportDAO(String siteShortname, String siteTitle, String powerUsers) {
    // TODO Auto-generated constructor stub
    this.siteShortname = siteShortname;
    this.siteTitle = siteTitle;
    this.powerUsers = powerUsers;
    
  }
  @Override
  public String toString() {
    return "CreatePowerUserReportDAO [siteShortname=" + siteShortname + ", siteTitle=" + siteTitle + ", powerUsers=" + powerUsers + "]";
}
  public String getSiteShortname() {
    return siteShortname;
  }
  public String getSiteTitle() {
    return siteTitle;
  }
  public void setSiteTitle(String siteTitle) {
    this.siteTitle = siteTitle;
  }
  public void setSiteShortname(String siteShortname) {
    this.siteShortname = siteShortname;
  }
  public String getPowerUsers() {
    return powerUsers;
  }
  public void setPowerUsers(String powerUsers) {
    this.powerUsers = powerUsers;
  }
  
  
}
