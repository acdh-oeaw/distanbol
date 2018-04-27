package at.ac.oeaw.Viewable;

import java.util.ArrayList;

public class Viewable
{
    private String id;
    private ArrayList<String> types;
    private String depiction;
    private String comment;
    private String label;
    private String lattitude;
    private String longitude;

    public Viewable(String id, ArrayList<String> types, String depiction, String comment, String label, String lattitude, String longitude)
    {
        this.id = id;
        this.types = types;
        this.depiction = depiction;
        this.comment = comment;
        this.label = label;
        this.lattitude = lattitude;
        this.longitude = longitude;
    }

    public String getComment()
    {
        return this.comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public String getDepiction()
    {
        return this.depiction;
    }

    public void setDepiction(String depiction)
    {
        this.depiction = depiction;
    }

    public ArrayList<String> getTypes()
    {
        return this.types;
    }

    public void setTypes(ArrayList<String> types)
    {
        this.types = types;
    }

    public String getId()
    {
        return this.id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getLabel()
    {
        return this.label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public String getLatitude()
    {
        return this.lattitude;
    }

    public void setLattitude(String latitude)
    {
        this.lattitude = latitude;
    }

    public String getLongitude()
    {
        return this.longitude;
    }

    public void setLongitude(String longitude)
    {
        this.longitude = longitude;
    }
}
