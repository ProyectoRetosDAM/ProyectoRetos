package org.iesmurgi.reta2.Chat;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;


/**
 * Viewmodel que usamos para el historial del chat.
 * @author Alberto Fernández
 * @author Santiago Álvarez
 * @author Joaquín Pérez
 * @see ChatAdapter
 * @see ChatAdminActivity
 */

public class ChatModel extends ViewModel {

   MutableLiveData<String> texto = new MutableLiveData<>();

    /**
     * Método que devuelve el texto
     * @return texto
     */
    public MutableLiveData<String> getTexto() {
        return texto;
    }

    /**
     * Método que asigna un valor al texto
     * @param texto
     */
    public void setTexto(MutableLiveData<String> texto) {
        this.texto = texto;
    }
}
