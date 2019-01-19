package ch.heigvd.wns.security.jwt;

public class AccountCredentials {

    private String username;
    private String password;
    private String firstname;
    private String lastname;
    private String realUsername;

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getRealUsername() {
        return realUsername;
    }

    public void setRealUsername(String realUsername) {
        this.realUsername = realUsername;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }

    public void setUsername(String _username) { this.username = _username; }
    public void setPassword(String _password) { this.password = _password; }
}
