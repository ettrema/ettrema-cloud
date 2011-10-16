package com.ettrema.backup.account;

import com.ettrema.backup.config.Config;
import com.ettrema.httpclient.Host;
import com.ettrema.httpclient.HttpException;
import com.ettrema.httpclient.NotFoundException;
import com.ettrema.httpclient.ProxyDetails;
import com.ettrema.httpclient.Unauthorized;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class AccountCreator {

    private static final Logger log = LoggerFactory.getLogger( AccountCreator.class );

    private static Pattern p = Pattern.compile( "[a-z][a-z0-9/./-]*" );
    private final Config config;

	
    public AccountCreator( Config config ) {
        this.config = config;
    }


    public String getBaseDomain() {
        return config.getBaseDomain();
    }

    public int getPort() {
        return config.getPort(); 
    }

    public String getSites() {
        return config.getSites();
    }
    

    public boolean create( JComponent component, Field hostName, Field email, Field name, Field password, Field nickName, ProxyDetails proxyDetails ) {
        Host host = new Host( hostName.getValue(), getPort(), null, null, proxyDetails );
        Map<String, String> params = new HashMap<String, String>();
        params.put( "create", "create" );
        params.put( "email", email.getValue() );
        params.put( "newName", name.getValue() );
        params.put( "password", password.getValue() );
        params.put( "nickName", nickName.getValue() ); // todo

        String jsonTxt;
        try {
            jsonTxt = host.post( "index.html/create", params );
        } catch( HttpException ex ) {
			log.error("Failed to connect to the website", ex);
            JOptionPane.showMessageDialog( component, "Couldnt connect to the website", "Connect failed", JOptionPane.ERROR_MESSAGE );
            return false;
        }
        if( jsonTxt == null || jsonTxt.trim().length() == 0 ) {
            return true;
        } else {
            JSONObject json = (JSONObject) JSONSerializer.toJSON( jsonTxt );
            JSONObject fieldErrors = json.getJSONObject( "fieldErrors" );
            if( fieldErrors.keySet().isEmpty() ) {
                return true;
            } else {
                for( Object oKey : fieldErrors.keySet() ) {
                    String key = (String) oKey;
                    String val = (String) fieldErrors.get( oKey );
                    if( key.equals( "email" ) ) {
                        email.setValidationError( val );
                    } else if( key.equals( "newName" ) ) {
                        name.setValidationError( val );
                    } else if( key.equals( "password" ) ) {
                        password.setValidationError( val );
                    } else {
                        throw new RuntimeException( "Unknown field: " + key );
                    }

                }
                return false;
            }
        }
    }

    public String validateName( String name ) {
        String s = "The account name ";

        if( name == null || name.length() == 0 ) {
            return s + " cannot be empty";
        }
        if( name.contains( " " ) ) {
            return s + " cannot contain spaces";
        }
        if( name.contains( "." ) ) {
            return s + " cannot contain dots";
        }
        Matcher m = p.matcher( name );
        if( !m.matches() ) {
            return "Please enter a name only consisting of lowercase letters and numbers.";
        }
        return null;
    }

    public boolean checkExists( String hostName, String accountName, ProxyDetails proxyDetails ) throws ConnectException {
        log.trace("checkExists: " + accountName + " on server: " + hostName);
         
        Host host = new Host( hostName, getPort(), null, null, proxyDetails );
        String accountPath = config.getMediaLoungePath(accountName);
		System.out.println("accountPath: " + accountPath);
        try {
            host.options( accountPath );
            log.trace( "found, does exist");
            return true;
        } catch( SocketTimeoutException ex ) {
            log.error("cant connect", ex);
            throw new ConnectException();
        } catch( UnknownHostException ex ) {
            log.error("cant connect", ex);
            throw new ConnectException();
        } catch( NotFoundException e ) {
            log.trace( "not found, does not exist", e);
            return false;
        } catch( Unauthorized e) {
            log.trace("not authorised, so does exist", e);
            return true;
        } catch( IOException ex ) {
            log.error("cant connect", ex);
            throw new ConnectException();

        } catch( HttpException ex ) {
            log.error("cant connect", ex);
            throw new ConnectException();

        }
    }

    public String getAccountDomain(String name) {
        return name + "." + getBaseDomain();
    }

    public String validatePassword( String password ) {
        if( password == null || password.length() == 0 ) {
            return "Please enter a password";
        } else {
            if( password.length() < 6) {
                return "Please enter a password of at least six characters";
            } else {
                return null;
            }
        }
    }

	public String validateHost(String hostName) {
		if( hostName == null || hostName.length() == 0 ) {
			return "Please enter a host name like: www.ettrema.com";
		}
		// TODO ping the server
		return null;
	}

    public static class Field {

        public Field( String value ) {
            this.value = value;
        }
        private String value;
        private String validationError;

        /**
         * @return the value
         */
        public String getValue() {
            return value;
        }

        /**
         * @param value the value to set
         */
        public void setValue( String value ) {
            this.value = value;
        }

        /**
         * @return the validationError
         */
        public String getValidationError() {
            return validationError;
        }

        /**
         * @param validationError the validationError to set
         */
        public void setValidationError( String validationError ) {
            this.validationError = validationError;
        }
    }
}
