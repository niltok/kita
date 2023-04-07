package ikuyo.api.hooks;

import com.fasterxml.jackson.annotation.JsonBackReference;
import ikuyo.api.datatypes.UserInfo;

public class UserHook extends AbstractHook {
    @JsonBackReference
    protected UserInfo userInfo;
    public UserHook() {}
    public UserHook(UserInfo userInfo) {
        this.userInfo = userInfo;
    }
}
