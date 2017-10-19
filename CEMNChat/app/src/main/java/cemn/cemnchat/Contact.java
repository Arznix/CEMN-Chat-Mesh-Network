package cemn.cemnchat;

/**
 * Created by Milad on 9/16/2017.
 */

public class Contact {
    private String cid;

    public Contact(String contactCid )
    {
        cid = contactCid;
    }

    public String getCid()
    {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }
}
