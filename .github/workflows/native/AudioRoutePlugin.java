package com.caveirax.micbooster;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.ArrayList;
import java.util.List;

/**
 * v3 - microfone e saida escolhidos separadamente.
 *
 * Regra de ouro: o modo "chamada" do Android (MODE_IN_COMMUNICATION) so e usado
 * quando e realmente necessario, porque ele derruba a qualidade (Bluetooth vira
 * 8/16 kHz, entra cancelamento de eco/AGC do aparelho). No modo normal o app capta
 * em banda cheia e o som sai pela saida de midia (alto-falante, fone, caixa A2DP).
 */
@CapacitorPlugin(name = "AudioRoute")
public class AudioRoutePlugin extends Plugin {

    private AudioManager am() {
        return (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
    }

    // tipos do AudioDeviceInfo (numeros fixos para nao depender do nivel da API)
    private static final int T_SPEAKER = 2, T_WIRED = 3, T_PHONES = 4, T_SCO = 7, T_A2DP = 8,
            T_USB_DEV = 11, T_BUILTIN_MIC = 15, T_USB_HEADSET = 22, T_HEARING = 23,
            T_BLE_HEADSET = 26, T_BLE_SPEAKER = 27;

    private String kind(int t) {
        switch (t) {
            case T_SPEAKER: case T_BUILTIN_MIC: return "celular";
            case T_WIRED: case T_PHONES: return "fone com fio";
            case T_USB_DEV: case T_USB_HEADSET: return "usb";
            case T_SCO: case T_BLE_HEADSET: return "bluetooth chamada";
            case T_A2DP: case T_BLE_SPEAKER: return "bluetooth som";
            case T_HEARING: return "aparelho auditivo";
            default: return "outro";
        }
    }

    private boolean isCall(int t) { return t == T_SCO || t == T_BLE_HEADSET; }

    private boolean okIn(int t) {
        return t == T_BUILTIN_MIC || t == T_WIRED || t == T_USB_DEV || t == T_USB_HEADSET
                || t == T_SCO || t == T_BLE_HEADSET;
    }

    private boolean okOut(int t) {
        return t == T_SPEAKER || t == T_WIRED || t == T_PHONES || t == T_USB_DEV || t == T_USB_HEADSET
                || t == T_SCO || t == T_A2DP || t == T_BLE_HEADSET || t == T_BLE_SPEAKER || t == T_HEARING;
    }

    private List<AudioDeviceInfo> devs(int flag) {
        List<AudioDeviceInfo> l = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 23) {
            for (AudioDeviceInfo d : am().getDevices(flag)) l.add(d);
        }
        return l;
    }

    private AudioDeviceInfo byId(int flag, int id) {
        if (id < 0) return null;
        for (AudioDeviceInfo d : devs(flag)) if (d.getId() == id) return d;
        return null;
    }

    private JSObject info(AudioDeviceInfo d) {
        JSObject o = new JSObject();
        o.put("id", d.getId());
        o.put("name", String.valueOf(d.getProductName()));
        o.put("type", d.getType());
        o.put("kind", kind(d.getType()));
        return o;
    }

    @PluginMethod
    public void listDevices(PluginCall call) {
        try {
            JSArray ins = new JSArray();
            JSArray outs = new JSArray();
            for (AudioDeviceInfo d : devs(AudioManager.GET_DEVICES_INPUTS)) if (okIn(d.getType())) ins.put(info(d));
            for (AudioDeviceInfo d : devs(AudioManager.GET_DEVICES_OUTPUTS)) if (okOut(d.getType())) outs.put(info(d));
            JSObject r = new JSObject();
            r.put("inputs", ins);
            r.put("outputs", outs);
            call.resolve(r);
        } catch (Exception e) {
            call.reject(String.valueOf(e.getMessage()));
        }
    }

    /** acha o dispositivo de comunicacao (API 31+) com o tipo pedido, preferindo o mesmo endereco */
    private AudioDeviceInfo commDevice(int type, AudioDeviceInfo like) {
        if (Build.VERSION.SDK_INT < 31) return null;
        AudioDeviceInfo first = null;
        for (AudioDeviceInfo d : am().getAvailableCommunicationDevices()) {
            if (d.getType() != type) continue;
            if (like != null && d.getAddress() != null && d.getAddress().equals(like.getAddress())) return d;
            if (first == null) first = d;
        }
        return first;
    }

    private void normalMode(AudioManager m) {
        if (Build.VERSION.SDK_INT >= 31) m.clearCommunicationDevice();
        try { m.stopBluetoothSco(); } catch (Exception e) { /* ignora */ }
        m.setBluetoothScoOn(false);
        m.setSpeakerphoneOn(false);
        m.setMode(AudioManager.MODE_NORMAL);
    }

    @PluginMethod
    public void setRoute(PluginCall call) {
        try {
            Integer a = call.getInt("inId", -1), b = call.getInt("outId", -1);
            int inId = a == null ? -1 : a, outId = b == null ? -1 : b;
            AudioManager m = am();
            AudioDeviceInfo in = byId(AudioManager.GET_DEVICES_INPUTS, inId);
            AudioDeviceInfo out = byId(AudioManager.GET_DEVICES_OUTPUTS, outId);
            int it = in == null ? -1 : in.getType();
            int ot = out == null ? -1 : out.getType();

            boolean wantSpeaker = ot == T_SPEAKER;
            boolean inCall = isCall(it);
            boolean outCall = isCall(ot);
            boolean headsetPresent = false;
            for (AudioDeviceInfo d : devs(AudioManager.GET_DEVICES_OUTPUTS)) {
                int t = d.getType();
                if (t != T_SPEAKER && okOut(t)) headsetPresent = true;
            }

            String note;
            String mode;
            if (wantSpeaker && inCall) {
                // o Android nao deixa microfone Bluetooth de chamada com saida no alto-falante
                note = "O Android nao permite microfone Bluetooth de chamada com saida no alto-falante. Usei o microfone do celular e o alto-falante.";
                inCall = false;
                in = null;
            } else {
                note = "";
            }

            boolean needCall = inCall || outCall || (wantSpeaker && headsetPresent);
            if (!needCall) {
                normalMode(m);
                mode = "normal";
                if (note.isEmpty()) {
                    note = "Modo normal (alta qualidade). Microfone: "
                            + (in != null ? in.getProductName() : "automatico")
                            + ". Saida: " + (out != null ? out.getProductName() : "a que o Android escolher") + ".";
                }
            } else {
                m.setMode(AudioManager.MODE_IN_COMMUNICATION);
                mode = "chamada";
                if (wantSpeaker) {
                    if (Build.VERSION.SDK_INT >= 31) {
                        AudioDeviceInfo sp = commDevice(T_SPEAKER, null);
                        if (sp != null) m.setCommunicationDevice(sp);
                        else m.setSpeakerphoneOn(true);
                    } else {
                        try { m.stopBluetoothSco(); } catch (Exception e) { /* ignora */ }
                        m.setBluetoothScoOn(false);
                        m.setSpeakerphoneOn(true);
                    }
                    if (note.isEmpty()) {
                        note = "Saida no alto-falante do celular (modo chamada, microfone do celular)"
                                + (headsetPresent ? ", mesmo com fone conectado." : ".");
                    }
                } else {
                    AudioDeviceInfo like = inCall ? in : out;
                    if (Build.VERSION.SDK_INT >= 31) {
                        AudioDeviceInfo sco = commDevice(like.getType(), like);
                        if (sco == null) sco = commDevice(T_SCO, like);
                        if (sco == null) sco = commDevice(T_BLE_HEADSET, like);
                        if (sco != null) m.setCommunicationDevice(sco);
                    } else {
                        m.setSpeakerphoneOn(false);
                        m.startBluetoothSco();
                        m.setBluetoothScoOn(true);
                    }
                    note = "Microfone e saida no Bluetooth em modo chamada (qualidade de ligacao).";
                }
            }
            JSObject r = new JSObject();
            r.put("mode", mode);
            r.put("note", note);
            call.resolve(r);
        } catch (Exception e) {
            call.reject(String.valueOf(e.getMessage()));
        }
    }

    @PluginMethod
    public void reset(PluginCall call) {
        try {
            normalMode(am());
        } catch (Exception e) {
            // ignora
        }
        call.resolve();
    }
}
