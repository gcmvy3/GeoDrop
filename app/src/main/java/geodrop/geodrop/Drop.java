package geodrop.geodrop;


public class Drop
{
    protected double latitude;
    protected double longitude;
    protected String message;

    public Drop(double lat, double lon, String mes)
    {
        latitude = lat;
        longitude = lon;
        message = mes;
    }

    public String toString()
    {
        return message;
    }
}
