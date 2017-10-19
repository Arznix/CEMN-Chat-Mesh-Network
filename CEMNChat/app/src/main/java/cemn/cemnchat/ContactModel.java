package cemn.cemnchat;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Milad on 9/16/2017.
 */

public class ContactModel {

    private static ContactModel sContactModel;
    private List<Contact> mContacts;

    public static ContactModel get(Context context)
    {
        if(sContactModel == null)
        {
            sContactModel = new ContactModel(context);
        }
        return  sContactModel;
    }

    private ContactModel(Context context)
    {
        mContacts = new ArrayList<>();
        populateWithInitialContacts(context);

    }

    private void populateWithInitialContacts(Context context)
    {
        //Create the Foods and add them to the list;


        Contact contact1 = new Contact("milad2@localhost");
        mContacts.add(contact1);
        Contact contact2 = new Contact("harry@localhost");
        mContacts.add(contact2);
        Contact contact3 = new Contact("harry2@localhost");
        mContacts.add(contact3);
    }

    public List<Contact> getContacts()
    {
        return mContacts;
    }

}
