package org.iesmurgi.reta2.UI.admin;

public class PruebaLoc {
    private String latitud;
    private String longitud;

    public PruebaLoc(String latitud, String longitud) {
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public PruebaLoc() {
    }

    public String getLatitud() {
        return latitud;
    }

    public void setLatitud(String latitud) {
        this.latitud = latitud;
    }

    public String getLongitud() {
        return longitud;
    }

    public void setLongitud(String longitud) {
        this.longitud = longitud;
    }
}
