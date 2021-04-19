package cz.incad.kramerius.auth.thirdparty.social.utils;

import org.brickred.socialauth.Profile;

import cz.incad.kramerius.auth.thirdparty.impl.AbstractUsersWrapper;
import cz.incad.kramerius.security.utils.UserUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class OpenIdUserWrapper extends AbstractUsersWrapper {

    private Profile p;
    private String calcName;

    public OpenIdUserWrapper(String calcName, Profile p) {
        super();
        this.p = p;
        this.calcName = calcName;
    }

    @Override
    public String getCalculatedName() {
        return this.calcName;
    }

    @Override
    public Set<String> getPropertyKeys() {
        return new HashSet<>(Arrays.asList(UserUtils.FIRST_NAME_KEY, UserUtils.LAST_NAME_KEY));
    }

    @Override
    public String getProperty(String key) {
        if (key.equals(UserUtils.FIRST_NAME_KEY)) {
            return p.getFirstName();
        } else if (key.equals(UserUtils.LAST_NAME_KEY)) {
            return p.getLastName();
        } else
            return null;
    }
}
