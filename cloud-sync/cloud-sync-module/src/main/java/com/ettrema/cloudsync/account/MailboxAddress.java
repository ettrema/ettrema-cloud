
package com.ettrema.cloudsync.account;

import java.io.Serializable;

public class MailboxAddress implements Serializable{

    private static final long serialVersionUID = 1L;

    static String removeSurroundingDelimiters(String p, String delim1, String delim2) {
        int pos = p.indexOf(delim1);
        if (pos >= 0) {
            p = p.substring(pos + 1);
        }
        pos = p.indexOf(delim2);
        if (pos >= 0) {
            p = p.substring(0, pos);
        }
        return p;
    }

    public static MailboxAddress parse(String address) throws IllegalArgumentException {
        if( address == null  ) throw new IllegalArgumentException("address argument is null");
        if( address.length() == 0 ) throw new IllegalArgumentException("address argument is empty");

        int posOpenBracket = address.indexOf("<");
        if( posOpenBracket > 0 ) {
            String p = address.substring(0, posOpenBracket-1);
            p = removeSurroundingDelimiters(p, "\"", "\"");

            String add = address.substring(posOpenBracket+1);
            add = removeSurroundingDelimiters(add, "<", ">");
            String[] arr = add.split("[@]");
            if( arr.length != 2 ) throw new IllegalArgumentException("Not a valid email address: " + address);
            return new MailboxAddress(arr[0], arr[1],p);
        } else {
            String[] arr = address.split("[@]");
            if( arr.length != 2 ) throw new IllegalArgumentException("Not a valid email address: " + address);
            return new MailboxAddress(arr[0], arr[1]);
        }
    }

    public final String user;
    public final String domain;
    public final String personal;





    public MailboxAddress(String user, String domain, String personal) {
        this.user = user;
        this.domain = domain;
        this.personal = personal;
    }


    public MailboxAddress(String user, String domain) {
        this.user = user;
        this.domain = domain;
        this.personal = null;
    }

    @Override
    public String toString() {
        if( personal == null ) {
            return toPlainAddress();
        } else {
            return "\"" + personal + "\"" + " <" + toPlainAddress() + ">";
        }
    }

    public String toPlainAddress() {
        return user + "@" + domain;
    }

    public String getDomain() {
        return domain;
    }

    public String getPersonal() {
        return personal;
    }

    public String getUser() {
        return user;
    }

    /**
     * Returns a representative name for this address. This is the personal
     * portion if present, otherwise it is the user portion.
     * 
     * @return
     */
    public String getDisplayName() {
        if( personal != null && personal.length() > 0 ) {
            return personal;
        } else {
            return user;
        }
    }
    
    
}
