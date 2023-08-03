package com.rezins.datecs_printer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.datecs.api.printer.Printer;
import com.datecs.api.printer.ProtocolAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONException;

/** DatecsPrinterPlugin */
public class DatecsPrinterPlugin implements FlutterPlugin, MethodCallHandler {

  private MethodChannel channel;

  private Context mContext;

  private Printer mPrinter;
  private ProtocolAdapter mProtocolAdapter;
  private BluetoothAdapter mBluetoothAdapter;
  private BluetoothSocket mmSocket;
  private BluetoothDevice mmDevice;
  private OutputStream mmOutputStream;
  private InputStream mmInputStream;

  private boolean isConnect = false;

  private final ProtocolAdapter.ChannelListener mChannelListener = new ProtocolAdapter.ChannelListener(){
    @Override
    public void onReadEncryptedCard() {
      // TODO: onReadEncryptedCard
    }

    @Override
    public void onReadCard() {
      // TODO: onReadCard
    }

    @Override
    public void onReadBarcode() {
      // TODO: onReadBarcode
    }

    @Override
    public void onPaperReady(boolean state) {
      if (state) {
      } else {
        try {
          disconnect();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    @Override
    public void onOverHeated(boolean state) {
      if (state) {
      }
    }
    // 6ca1a08e05c9439bbb6c2825ae7fdec4
    @Override
    public void onLowBattery(boolean state) {
      if (state) {
      }
    }
  };

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "datecs_printer");
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    } else if (call.method.equals("getListBluetoothDevice")){

      // Get the local Bluetooth adapter
      mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
      ArrayList<Map<String, String>> devices = new ArrayList<Map<String, String>>();
      if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
          Map<String, String> list = new HashMap<String, String>();
          list.put("name", device.getName());
          list.put("address", device.getAddress());
          devices.add(list);
        }
        result.success(devices);


      }else{

        result.error("Error 101", "Error while get list bluetooth device","");
      }

    }else if (call.method.equals("connectBluetooth")){
      String address = call.argument("address");
      try{
        if (BluetoothAdapter.checkBluetoothAddress(address)) {
          isConnect = connect(address);
        }
        result.success(isConnect);
      }catch(IOException e){
        result.success(isConnect);
      }catch(Exception e){
        result.success(isConnect);
      }

    }else if(call.method.equals("disconnectBluetooth")){
      try{
        disconnect();
        result.success(true);
      }catch(IOException e){
        result.success(false);
      }
    }else if(call.method.equals("testPrint")){
      try {
        mPrinter.printSelfTest();
        mPrinter.flush();
        result.success(true);
      } catch (Exception e) {
        result.success(false);
      }
    }else if(call.method.equals("printText")){
      String charset = "ISO-8859-1";
      List<String> args = call.argument("args");
      try {
        mPrinter.reset();
        for(int i = 0; i < args.size(); i++){
          if(args.get(i).contains("feed%20")){
            String[] split = args.get(i).split("%20");
            int feed = Integer.parseInt(split[1]);
            mPrinter.feedPaper(feed);
          }else if(args.get(i).contains("img%2021")){
            String[] split = args.get(i).split("%2021");
            String img = split[1];
            if(android.os.Build.VERSION.SDK_INT >= 26){
              byte[] decodedString = Base64.getDecoder().decode(img.getBytes("UTF-8"));
              Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
              Bitmap resized = Bitmap.createScaledBitmap(decodedByte, 300, 300, true);
              final int[] argb = new int[300 * 300];
              resized.getPixels(argb, 0, 300, 0, 0, 300, 300);
              resized.recycle();

              mPrinter.printImage(argb, 300, 300, Printer.ALIGN_CENTER, true);
            }else{
              byte[] decodedString = android.util.Base64.decode(img, android.util.Base64.DEFAULT);
              Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
              Bitmap resized = Bitmap.createScaledBitmap(decodedByte, 300, 300, true);
              final int[] argb = new int[300 * 300];
              resized.getPixels(argb, 0, 300, 0, 0, 300, 300);
              resized.recycle();
              mPrinter.printImage(argb, 300, 300, Printer.ALIGN_CENTER, true);
            }
          }else if(args.get(i).contains("img%danfe")){
            String[] split = args.get(i).split("%danfe");
            String img = split[1];
            if(android.os.Build.VERSION.SDK_INT >= 26){
              byte[] decodedString = Base64.getDecoder().decode(img.getBytes("UTF-8"));
              Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

              Bitmap resized = Bitmap.createScaledBitmap(decodedByte, 840, 1250, true);
              // decodedByte.recycle();
              final int width = resized.getWidth();
              final int height = resized.getHeight();
              final int[] argb = new int[width * height];
              resized.getPixels(argb, 0, width, 0, 0, width, height);
              resized.recycle();

              mPrinter.printCompressedImage(argb, width, height, Printer.ALIGN_CENTER, true);
            }else{
              byte[] decodedString = android.util.Base64.decode(img, android.util.Base64.DEFAULT);
              Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

              Bitmap resized = Bitmap.createScaledBitmap(decodedByte, 840, 1250, true);
              // decodedByte.recycle();
              final int width = resized.getWidth();
              final int height = resized.getHeight();
              final int[] argb = new int[width * height];
              resized.getPixels(argb, 0, width, 0, 0, width, height);
              resized.recycle();

              mPrinter.printCompressedImage(argb, width, height, Printer.ALIGN_CENTER, true);
            }
          }else if(args.get(i).contains("img%boleto")){
            String[] split = args.get(i).split("%boleto");
            String img = split[1];
            if(android.os.Build.VERSION.SDK_INT >= 26){
              byte[] decodedString = Base64.getDecoder().decode(img.getBytes("UTF-8"));
              Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

              Bitmap resized = Bitmap.createScaledBitmap(decodedByte, 840, 1400, true);
              // decodedByte.recycle();
              final int width = resized.getWidth();
              final int height = resized.getHeight();
              final int[] argb = new int[width * height];
              resized.getPixels(argb, 0, width, 0, 0, width, height);
              resized.recycle();

              mPrinter.printCompressedImage(argb, width, height, Printer.ALIGN_CENTER, true);
            }else{
              byte[] decodedString = android.util.Base64.decode(img, android.util.Base64.DEFAULT);
              Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

              Bitmap resized = Bitmap.createScaledBitmap(decodedByte, 840, 1400, true);
              // decodedByte.recycle();
              final int width = resized.getWidth();
              final int height = resized.getHeight();
              final int[] argb = new int[width * height];
              resized.getPixels(argb, 0, width, 0, 0, width, height);
              resized.recycle();

              mPrinter.printCompressedImage(argb, width, height, Printer.ALIGN_CENTER, true);
            }
          }else if(args.get(i).contains("print%danfe")){
            String[] split = args.get(i).split("%danfe");
            String jsonString = split[1];

            String emitNome = "";
            String emitCNPJ = "";
            String emitIe = "";
            String emitTel = "";
            String emitEnd = "";
            String numero = "";
            String serie = "";
            String chave = "";
            String protocolo = "";
            String natOp = "";
            String destRazao = "";
            String destEnd = "";
            String destDocument = "";
            String destIe = "";
            String emissao = "";
            String dSaiEnt = "";
            String hSaiEnt = "";
            String bcIcms = "";
            String vIcms = "";
            String bcIcmsSt = "";
            String vIcmsSt = "";
            String ttProdutos = "";
            String vFrete = "";
            String vSeguro = "";
            String vDesc = "";
            String vOutro = "";
            String vIPI = "";
            String ttNota = "";
            String adicionais = "";
            String logo = "";
            try {
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONObject invoice = jsonObject;
            JSONArray produtosArray = invoice.getJSONArray("produtos");
            JSONArray faturasArray = invoice.getJSONArray("faturas");

            emitNome = invoice.getString("emitNome");
            emitCNPJ = invoice.getString("emitCNPJ");
            emitIe = invoice.getString("emitIe");
            emitTel = invoice.getString("emitTel");
            emitEnd = invoice.getString("emitEnd");
            numero = invoice.getString("numero");
            serie = invoice.getString("serie");
            chave = invoice.getString("chave");
            protocolo = invoice.getString("protocolo");
            natOp = invoice.getString("natOp");
            destRazao = invoice.getString("destRazao");
            destEnd = invoice.getString("destEnd");
            destDocument = invoice.getString("destDocument");
            destIe = invoice.getString("destIe");
            emissao = invoice.getString("dhEmi");
            dSaiEnt = invoice.getString("dSaiEnt");
            hSaiEnt = invoice.getString("hSaiEnt");
            bcIcms = invoice.getString("bcIcms");
            bcIcms = invoice.getString("vIcms");
            bcIcmsSt = invoice.getString("bcIcmsSt");
            vIcmsSt = invoice.getString("vIcmsSt");
            ttProdutos = invoice.getString("ttProdutos");
            vFrete = invoice.getString("vFrete");
            vSeguro = invoice.getString("vSeguro");
            vDesc = invoice.getString("vDesc");
            vOutro = invoice.getString("vOutro");
            vIPI = invoice.getString("vIPI");
            ttNota = invoice.getString("ttNota");
            adicionais = invoice.getString("adicionais");
            logo = invoice.getString("company_logo_base64");
            
            mPrinter.flush();
            mPrinter.reset();
            mPrinter.selectPageMode();
            mPrinter.setPageRegion(0, 0, 650, 220, Printer.PAGE_LEFT);
            mPrinter.drawPageFrame(0, 0, 650, 100, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(5, 5);

            mPrinter.printTaggedText("{reset}Recebemos de " + emitNome
                            + " os produtos constantes da nota fiscal indicada ao lado.{br}");
            mPrinter.setPageXY(0, 105);
            mPrinter.printTaggedText("{reset}{center}{b}DATA E HORA DO RECEBIMENTO{br}");
            mPrinter.drawPageRectangle(0, 100, 650, 32, Printer.FILL_INVERTED);
            mPrinter.drawPageFrame(0, 100, 650, 120, Printer.FILL_BLACK, 2);

            mPrinter.setPageRegion(650, 0, 150, 220, Printer.PAGE_LEFT);
            mPrinter.printTaggedText("{reset}{br}{center}{h} 1 - Saida {br}{center}NF-e{br}{center}N " + numero + "{br}{center}Serie " + serie, "UTF-8");
            mPrinter.drawPageFrame(0, 0, 150, 220, Printer.FILL_BLACK, 2);

            mPrinter.setPageRegion(0, 220, 800, 120, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}{b}IDENTIFICACAO E ASSINATURA DO RECEBEDOR{br}");
            mPrinter.drawPageRectangle(0, 0, 800, 32, Printer.FILL_INVERTED);
            mPrinter.drawPageFrame(0, 0, 800, 120, Printer.FILL_BLACK, 2);

            mPrinter.setPageRegion(0, 340, 800, 32, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{b}--------------------------------------------------------------------{br}");
            mPrinter.feedPaper(5);
            int y = 372;

            //REMOVER
            mPrinter.printPage();
            mPrinter.flush();
            mPrinter.reset();
            mPrinter.selectStandardMode();
            mPrinter.selectPageMode();
            mPrinter.flush();
            //REMOVER

            mPrinter.setPageRegion(0, y, 650, 280, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.drawPageFrame(0, 0, 650, 280, Printer.FILL_BLACK, 2);
            
            if (logo != null) {
              if(android.os.Build.VERSION.SDK_INT >= 26){
                byte[] decodedString = Base64.getDecoder().decode(logo.getBytes("UTF-8"));
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                Bitmap resized = Bitmap.createScaledBitmap(decodedByte, 200, 144, true);
                final int width = resized.getWidth();
                final int height = resized.getHeight();
                final int[] argb = new int[width * height];
                resized.getPixels(argb, 0, width, 0, 0, width, height);
                resized.recycle();

                mPrinter.printCompressedImage(argb, width, height, Printer.ALIGN_LEFT, true);
              }else{
                byte[] decodedString = android.util.Base64.decode(logo, android.util.Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                Bitmap resized = Bitmap.createScaledBitmap(decodedByte, 200, 144, true);
                final int width = resized.getWidth();
                final int height = resized.getHeight();
                final int[] argb = new int[width * height];
                resized.getPixels(argb, 0, width, 0, 0, width, height);
                resized.recycle();

                mPrinter.printCompressedImage(argb, width, height, Printer.ALIGN_LEFT, true);
              }
            }

            mPrinter.setPageXY(0, 30);
            mPrinter.printTaggedText("{reset}{br}{right}{h}{w}DANFE SIMPLIFICADO{br}{center}{/w}{s}{right}Documento Auxiliar de Nota Fiscal Eletronica{br}");
            mPrinter.setPageXY(0, 130);
            mPrinter.printTaggedText("{reset}{br}{b}" + emitNome + "{/b}{br}" + emitCNPJ + " " + emitIe + " " + emitTel + "{br}" + emitEnd + "{br}");

            mPrinter.setPageRegion(650, y, 150, 280, Printer.PAGE_LEFT);
            mPrinter.printTaggedText("{reset}{br}{center}{h} 1 - Saida {br}{center}NF-e{br}{center}N " + numero + "{br}{center}Serie " + serie, "UTF-8");
            mPrinter.drawPageFrame(0, 0, 150, 280, Printer.FILL_BLACK, 2);
            y += 280;

            mPrinter.setPageRegion(0, y, 800, 64, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}{b}CHAVE DE ACESSO{br}");
            mPrinter.drawPageRectangle(0, 0, 800, 32, Printer.FILL_INVERTED);
            mPrinter.drawPageFrame(0, 0, 800, 64, Printer.FILL_BLACK, 2);
            String s = chave.replaceAll("(....(?!\\z))", "$1 ");
            mPrinter.setPageXY(0, 37);
            mPrinter.printTaggedText("{reset}{center}" + s + "{br}");
            y += 64 + 15;

            mPrinter.setPageRegion(0, y, 800, 100, Printer.PAGE_LEFT);
            mPrinter.setBarcode(Printer.ALIGN_CENTER, true, 2, Printer.HRI_BELOW, 100);
            mPrinter.printBarcode(Printer.BARCODE_CODE128AUTO, chave);

            y += 100 + 15;

            mPrinter.setPageRegion(0, y, 800, 64, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}{b}PROTOCOLO DE AUTORIZACAO DE USO{br}");
            mPrinter.drawPageRectangle(0, 0, 800, 32, Printer.FILL_INVERTED);
            mPrinter.drawPageFrame(0, 0, 800, 64, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 37);
            mPrinter.printTaggedText("{reset}{center}" + protocolo + "{br}");
            y += 64 + 15;

            mPrinter.setPageRegion(0, y, 800, 64, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}{b}NATUREZA DA OPERACAO{br}");
            mPrinter.drawPageRectangle(0, 0, 800, 32, Printer.FILL_INVERTED);
            mPrinter.drawPageFrame(0, 0, 800, 64, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 37);
            mPrinter.printTaggedText("{reset}{center}" + natOp + "{br}");
            y += 64 + 15;

            mPrinter.setPageRegion(0, y, 650, 220, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}{b}DESTINATARIO{br}");
            mPrinter.drawPageRectangle(0, 0, 650, 32, Printer.FILL_INVERTED);
            mPrinter.drawPageFrame(0, 0, 650, 220, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 37);

            mPrinter.printTaggedText("{reset}{left}{b}" + destRazao + "{/b}{br}" + destEnd + "{br}CNPJ/CPF: " + destDocument + "{br}I.E.:" + destIe + "{br}");

            mPrinter.setPageRegion(650, y, 150, 73, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 10);
            mPrinter.printTaggedText("{reset}{center}EMISSAO{br}{center}" + emissao + "{br}");
            mPrinter.drawPageFrame(0, 0, 150, 73, Printer.FILL_BLACK, 2);
            mPrinter.setPageRegion(650, y + 73, 150, 73, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 10);
            mPrinter.printTaggedText("{reset}{center}SAIDA{br}{center}" + dSaiEnt + "{br}");
            mPrinter.drawPageFrame(0, 0, 150, 73, Printer.FILL_BLACK, 2);
            mPrinter.setPageRegion(650, y + 73 + 73, 150, 74, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 10);
            mPrinter.printTaggedText("{reset}{center}HORA{br}{center}" + hSaiEnt + "{br}");
            mPrinter.drawPageFrame(0, 0, 150, 74, Printer.FILL_BLACK, 2);
            y += 220 + 15;

            mPrinter.setPageRegion(0, y, 800, 32, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}{b}FATURAS{br}");
            mPrinter.drawPageRectangle(0, 0, 800, 32, Printer.FILL_INVERTED);
            y += 32;

            //REMOVER
            mPrinter.printPage();
            mPrinter.flush();
            mPrinter.reset();
            mPrinter.selectStandardMode();
            mPrinter.selectPageMode();
            mPrinter.flush();
            //REMOVER

            for (int k = 0; k < faturasArray.length(); k++) {
              JSONObject fatura = faturasArray.getJSONObject(k);

              mPrinter.setPageRegion(0, y, 800, 32, Printer.PAGE_LEFT);
              mPrinter.setPageXY(0, 5);
              String si = String.valueOf(k + 1) + " - " + fatura.getString("dVenc") + " R$ " + fatura.getString("vDup");

              mPrinter.drawPageFrame(0, 0, 800, 32, Printer.FILL_BLACK, 2);
              if (k + 1 < faturasArray.length()) {
                  k++;
                  si += "  |  " + String.valueOf(i + 1) + " - " + fatura.getString("dVenc") + " R$ " + fatura.getString("vDup");
              }
              mPrinter.printTaggedText("{reset}{left}" + si + "{br}");
              y += 32;
            }
            y += 15;

            mPrinter.printPage();
            mPrinter.flush();
            mPrinter.reset();
            mPrinter.selectStandardMode();
            mPrinter.selectPageMode();
            mPrinter.flush();
            y = 0;

            mPrinter.setPageRegion(0, y, 800, 32, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}{b}IMPOSTO{br}");
            mPrinter.drawPageRectangle(0, 0, 800, 32, Printer.FILL_INVERTED);
            y += 32;

            mPrinter.setPageRegion(0, y, 150, 74, Printer.PAGE_LEFT);
            mPrinter.drawPageFrame(0, 0, 150, 30, Printer.FILL_BLACK, 2);
            mPrinter.drawPageFrame(0, 30, 150, 44, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}B.C.ICMS{br}");
            mPrinter.setPageXY(0, 48);
            mPrinter.printTaggedText("{reset}{right}" + bcIcms + "{br}");
            int x = 150;

            mPrinter.setPageRegion(x, y, 150, 74, Printer.PAGE_LEFT);
            mPrinter.drawPageFrame(0, 0, 150, 30, Printer.FILL_BLACK, 2);
            mPrinter.drawPageFrame(0, 30, 150, 44, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}V.ICMS{br}");
            mPrinter.setPageXY(0, 48);
            mPrinter.printTaggedText("{reset}{right}" + vIcms + "{br}");
            x += 150;

            mPrinter.setPageRegion(x, y, 150, 74, Printer.PAGE_LEFT);
            mPrinter.drawPageFrame(0, 0, 150, 30, Printer.FILL_BLACK, 2);
            mPrinter.drawPageFrame(0, 30, 150, 44, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}B.C.ICMS.ST{br}");
            mPrinter.setPageXY(0, 48);
            mPrinter.printTaggedText("{reset}{right}" + bcIcmsSt + "{br}");
            x += 150;

            mPrinter.setPageRegion(x, y, 150, 74, Printer.PAGE_LEFT);
            mPrinter.drawPageFrame(0, 0, 150, 30, Printer.FILL_BLACK, 2);
            mPrinter.drawPageFrame(0, 30, 150, 44, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}V.ICMS.ST{br}");
            mPrinter.setPageXY(0, 48);
            mPrinter.printTaggedText("{reset}{right}" + vIcmsSt + "{br}");
            x += 150;

            mPrinter.setPageRegion(x, y, 200, 74, Printer.PAGE_LEFT);
            mPrinter.drawPageFrame(0, 0, 200, 30, Printer.FILL_BLACK, 2);
            mPrinter.drawPageFrame(0, 30, 200, 44, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}TOTAL PRODUTOS{br}");
            mPrinter.setPageXY(0, 48);
            mPrinter.printTaggedText("{reset}{right}" + ttProdutos + "{br}");
            x = 0;
            y += 74;

            mPrinter.setPageRegion(x, y, 120, 74, Printer.PAGE_LEFT);
            mPrinter.drawPageFrame(0, 0, 120, 30, Printer.FILL_BLACK, 2);
            mPrinter.drawPageFrame(0, 30, 120, 44, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}V.FRETE{br}");
            mPrinter.setPageXY(0, 48);
            mPrinter.printTaggedText("{reset}{right}" + vFrete + "{br}");
            x += 120;

            mPrinter.setPageRegion(x, y, 120, 74, Printer.PAGE_LEFT);
            mPrinter.drawPageFrame(0, 0, 120, 30, Printer.FILL_BLACK, 2);
            mPrinter.drawPageFrame(0, 30, 120, 44, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}V.SEGURO{br}");
            mPrinter.setPageXY(0, 48);
            mPrinter.printTaggedText("{reset}{right}" + vSeguro + "{br}");
            x += 120;

            mPrinter.setPageRegion(x, y, 120, 74, Printer.PAGE_LEFT);
            mPrinter.drawPageFrame(0, 0, 120, 30, Printer.FILL_BLACK, 2);
            mPrinter.drawPageFrame(0, 30, 120, 44, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}DESCONTO{br}");
            mPrinter.setPageXY(0, 48);
            mPrinter.printTaggedText("{reset}{right}" + vDesc + "{br}");
            x += 120;

            mPrinter.setPageRegion(x, y, 120, 74, Printer.PAGE_LEFT);
            mPrinter.drawPageFrame(0, 0, 120, 30, Printer.FILL_BLACK, 2);
            mPrinter.drawPageFrame(0, 30, 120, 44, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}OUTROS{br}");
            mPrinter.setPageXY(0, 48);
            mPrinter.printTaggedText("{reset}{right}" + vOutro + "{br}");
            x += 120;

            mPrinter.setPageRegion(x, y, 120, 74, Printer.PAGE_LEFT);
            mPrinter.drawPageFrame(0, 0, 120, 30, Printer.FILL_BLACK, 2);
            mPrinter.drawPageFrame(0, 30, 120, 44, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}V.IPI{br}");
            mPrinter.setPageXY(0, 48);
            mPrinter.printTaggedText("{reset}{right}" + vIPI + "{br}");
            x += 120;

            mPrinter.setPageRegion(x, y, 200, 74, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}TOTAL GERAL{br}");
            mPrinter.drawPageRectangle(0, 0, 200, 30, Printer.FILL_INVERTED);
            mPrinter.drawPageFrame(0, 30, 200, 44, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 35);
            mPrinter.printTaggedText("{reset}{right}{h}" + ttNota + "{br}");
            x = 0;
            y += 74 + 15;

            mPrinter.setPageRegion(0, y, 800, 32, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}{b}PRODUTOS{br}");
            mPrinter.drawPageRectangle(0, 0, 800, 32, Printer.FILL_INVERTED);
            y += 32;

            mPrinter.setPageRegion(x, y, 40, 30, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}COD{br}");
            mPrinter.drawPageFrame(0, 0, 40, 30, Printer.FILL_BLACK, 2);
            x += 40;

            mPrinter.setPageRegion(x, y, 300, 30, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}DESCRICAO{br}");
            mPrinter.drawPageFrame(0, 0, 300, 30, Printer.FILL_BLACK, 2);
            x += 300;

            mPrinter.setPageRegion(x, y, 100, 30, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}NCM/SH{br}");
            mPrinter.drawPageFrame(0, 0, 100, 30, Printer.FILL_BLACK, 2);
            x += 100;

            mPrinter.setPageRegion(x, y, 40, 30, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}CST{br}");
            mPrinter.drawPageFrame(0, 0, 40, 30, Printer.FILL_BLACK, 2);
            x += 40;

            mPrinter.setPageRegion(x, y, 50, 30, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}CFOP{br}");
            mPrinter.drawPageFrame(0, 0, 50, 30, Printer.FILL_BLACK, 2);
            x += 50;

            mPrinter.setPageRegion(x, y, 40, 30, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}UN{br}");
            mPrinter.drawPageFrame(0, 0, 40, 30, Printer.FILL_BLACK, 2);
            x += 40;

            mPrinter.setPageRegion(x, y, 50, 30, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}QTDE{br}");
            mPrinter.drawPageFrame(0, 0, 50, 30, Printer.FILL_BLACK, 2);
            x += 50;

            mPrinter.setPageRegion(x, y, 80, 30, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}V.UNIT.{br}");
            mPrinter.drawPageFrame(0, 0, 80, 30, Printer.FILL_BLACK, 2);
            x += 80;

            mPrinter.setPageRegion(x, y, 105, 30, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}V.TOTAL{br}");
            mPrinter.drawPageFrame(0, 0, 105, 30, Printer.FILL_BLACK, 2);
            y += 30;

            for (int j = 0; j < produtosArray.length(); j++) {
                JSONObject produto = produtosArray.getJSONObject(j);

                 x = 0;
                mPrinter.setPageRegion(x, y, 40, 30, Printer.PAGE_LEFT);
                mPrinter.setPageXY(0, 5);
                mPrinter.printTaggedText("{reset}{center}{s}" + produto.getString("cProd") + "{br}");
                mPrinter.drawPageFrame(0, 0, 40, 30, Printer.FILL_BLACK, 2);
                x += 40;

                mPrinter.setPageRegion(x, y, 300, 30, Printer.PAGE_LEFT);
                mPrinter.setPageXY(0, 5);
                mPrinter.printTaggedText("{reset}{left}{s}" + produto.getString("xProd")  + "{br}");
                mPrinter.drawPageFrame(0, 0, 300, 30, Printer.FILL_BLACK, 2);
                x += 300;

                mPrinter.setPageRegion(x, y, 100, 30, Printer.PAGE_LEFT);
                mPrinter.setPageXY(0, 5);
                mPrinter.printTaggedText("{reset}{center}{s}" + produto.getString("NCM") + "{br}");
                mPrinter.drawPageFrame(0, 0, 100, 30, Printer.FILL_BLACK, 2);
                x += 100;

                mPrinter.setPageRegion(x, y, 40, 30, Printer.PAGE_LEFT);
                mPrinter.setPageXY(0, 5);
                mPrinter.printTaggedText("{reset}{center}{s}" + produto.getString("CST") + "{br}");
                mPrinter.drawPageFrame(0, 0, 40, 30, Printer.FILL_BLACK, 2);
                x += 40;

                mPrinter.setPageRegion(x, y, 50, 30, Printer.PAGE_LEFT);
                mPrinter.setPageXY(0, 5);
                mPrinter.printTaggedText("{reset}{center}{s}" + produto.getString("CFOP") + "{br}");
                mPrinter.drawPageFrame(0, 0, 50, 30, Printer.FILL_BLACK, 2);
                x += 50;

                mPrinter.setPageRegion(x, y, 40, 30, Printer.PAGE_LEFT);
                mPrinter.setPageXY(0, 5);
                mPrinter.printTaggedText("{reset}{center}{s}" + produto.getString("uTrib")  + "{br}");
                mPrinter.drawPageFrame(0, 0, 40, 30, Printer.FILL_BLACK, 2);
                x += 40;

                mPrinter.setPageRegion(x, y, 50, 30, Printer.PAGE_LEFT);
                mPrinter.setPageXY(0, 5);
                mPrinter.printTaggedText("{reset}{center}{s}" + produto.getString("qCom") + "{br}");
                mPrinter.drawPageFrame(0, 0, 50, 30, Printer.FILL_BLACK, 2);
                x += 50;

                mPrinter.setPageRegion(x, y, 80, 30, Printer.PAGE_LEFT);
                mPrinter.setPageXY(0, 5);
                mPrinter.printTaggedText("{reset}{right}{s}" + produto.getString("vUnCom") + "{br}");
                mPrinter.drawPageFrame(0, 0, 80, 30, Printer.FILL_BLACK, 2);
                x += 80;

                mPrinter.setPageRegion(x, y, 105, 30, Printer.PAGE_LEFT);
                mPrinter.setPageXY(0, 5);
                mPrinter.printTaggedText("{reset}{right}{s}" + produto.getString("vProd") + "{br}");
                mPrinter.drawPageFrame(0, 0, 105, 30, Printer.FILL_BLACK, 2);
                y += 30;
            }

            mPrinter.setPageRegion(0, y, 800, 220, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}{b}DADOS ADICIONAIS{br}");
            mPrinter.drawPageRectangle(0, 0, 800, 32, Printer.FILL_INVERTED);
            mPrinter.drawPageFrame(0, 0, 800, 220, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 37);
            mPrinter.printTaggedText("{reset}{left}" + adicionais + "{br}");

            mPrinter.printPage();
            mPrinter.selectStandardMode();
            mPrinter.feedPaper(110);
            mPrinter.flush();
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }else if(args.get(i).contains("print%boletos")){
            String[] split = args.get(i).split("%boletos");
            String jsonString = split[1];
            String logo = "";
            try {
              JSONObject jsonObject = new JSONObject(jsonString);
              JSONObject data = jsonObject;
              JSONArray installmentsArray = data.getJSONArray("installments");
              logo = data.getString("bank_logo");

              // mPrinter.reset();
              for (int j = 0; j < installmentsArray.length(); j++) {
                //REMOVER
                mPrinter.reset();
                mPrinter.selectPageMode();
                mPrinter.flush();
                mPrinter.printPage();
                //REMOVER

                JSONObject installment = installmentsArray.getJSONObject(j);

                 mPrinter.selectPageMode();
                  int y = 0;
                  //logo
                  mPrinter.setPageRegion(50, 0, 270, 70, Printer.PAGE_LEFT);

                  if (logo != null) {
                    if(android.os.Build.VERSION.SDK_INT >= 26){
                      byte[] decodedString = Base64.getDecoder().decode(logo.getBytes("UTF-8"));
                      Bitmap bitm = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                      final int wid = bitm.getWidth();
                      final int heig = bitm.getHeight();
                      final int[] ar = new int[wid * heig];
                      bitm.getPixels(ar, 0, wid, 0, 0, wid, heig);
                      bitm.recycle();

                      mPrinter.printCompressedImage(ar, wid, heig, Printer.ALIGN_LEFT, true);
                    }else{
                      byte[] decodedString = android.util.Base64.decode(logo, android.util.Base64.DEFAULT);
                      Bitmap bitm = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                      final int wid = bitm.getWidth();
                      final int heig = bitm.getHeight();
                      final int[] ar = new int[wid * heig];
                      bitm.getPixels(ar, 0, wid, 0, 0, wid, heig);
                      bitm.recycle();

                      mPrinter.printCompressedImage(ar, wid, heig, Printer.ALIGN_LEFT, true);
                    }
                  }
                  y += 270;

                  //numero banco
                  mPrinter.setPageRegion(310, 0, 700, 70, Printer.PAGE_LEFT);
                  mPrinter.setPageXY(8, 18);
                  mPrinter.drawPageFrame(0, 0, 150, 74, Printer.FILL_BLACK, 2);
                  mPrinter.printTaggedText("{reset}{left}{b}{w}{h}" + data.getString("bank_code") + "-" + data.getString("bank_digit") + "{reset}{h}{b}   COMPROVANTE DE ENTREGA{br}");

                  //Benificiario
                  mPrinter.setPageRegion(30, 70, 780, 63, Printer.PAGE_LEFT);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Benificiario{br}");
                  mPrinter.drawPageFrame(0, 0, 780, 63, Printer.FILL_BLACK, 2);
                  mPrinter.setPageXY(5, 33);
                  mPrinter.printTaggedText("{reset}{left}{h}{s}" + installment.getString("recipient") + " - CNPJ: " + installment.getString("cnpj") + "{br}");

                  //Pagador
                  mPrinter.setPageRegion(30, 131, 780, 63, Printer.PAGE_LEFT);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Pagador{br}");
                  mPrinter.drawPageFrame(0, 0, 780, 63, Printer.FILL_BLACK, 2);
                  mPrinter.setPageXY(5, 33);
                  mPrinter.printTaggedText("{reset}{left}{h}{s}" + installment.getString("paying_name") + " - CPF/CNPJ: " + installment.getString("document_client") + "{br}");

                  mPrinter.setPageRegion(30, 192, 150, 63, Printer.PAGE_LEFT);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Data Documento{br}");
                  mPrinter.drawPageFrame(0, 0, 150, 63, Printer.FILL_BLACK, 2);
                  mPrinter.setPageXY(5, 33);
                  mPrinter.printTaggedText("{reset}{center}{h}{s}" + installment.getString("financial_emission") + " {br}");

                  //Numero do Documento
                  mPrinter.setPageRegion(178, 192, 150, 63, Printer.PAGE_LEFT);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}N. Documento{br}");
                  mPrinter.drawPageFrame(0, 0, 150, 63, Printer.FILL_BLACK, 2);
                  mPrinter.setPageXY(5, 33);
                  mPrinter.printTaggedText("{reset}{center}{h}{s}" + installment.getString("financial_document") + " {br}");

                  //NVencimento
                  mPrinter.setPageRegion(326, 192, 140, 63, Printer.PAGE_LEFT);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Vencimento{br}");
                  mPrinter.drawPageFrame(0, 0, 140, 63, Printer.FILL_BLACK, 2);
                  mPrinter.setPageXY(5, 33);
                  mPrinter.printTaggedText("{reset}{center}{h}{s}" + installment.getString("due") + " {br}");

                  //Agencia/Cod.Beneficiario
                  mPrinter.setPageRegion(464, 192, 180, 63, Printer.PAGE_LEFT);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Nosso Numero{br}");
                  mPrinter.drawPageFrame(0, 0, 180, 63, Printer.FILL_BLACK, 2);
                  mPrinter.setPageXY(5, 33);
                  mPrinter.printTaggedText("{reset}{center}{h}{s}" + installment.getString("detail_our_number") + "-" + installment.getString("cod_num") + " {br}");

                  //Valor Documento
                  mPrinter.setPageRegion(642, 192, 168, 63, Printer.PAGE_LEFT);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Valor Documento{br}");
                  mPrinter.drawPageFrame(0, 0, 168, 63, Printer.FILL_BLACK, 2);
                  mPrinter.setPageXY(5, 33);
                  mPrinter.printTaggedText("{reset}{right}{h}{s}" + installment.getString("document_value") + " {br}");

                  //Assinatura
                  mPrinter.setPageRegion(30, 253, 270, 100, Printer.PAGE_LEFT);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Assinatura{br}");
                  mPrinter.drawPageFrame(0, 0, 270, 100, Printer.FILL_BLACK, 2);

                  //Data Recebimento
                  mPrinter.setPageRegion(298, 253, 270, 100, Printer.PAGE_LEFT);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Data Recebimento{br}");
                  mPrinter.drawPageFrame(0, 0, 270, 100, Printer.FILL_BLACK, 2);

                  //Dados
                  mPrinter.setPageRegion(566, 253, 244, 100, Printer.PAGE_LEFT);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Recebi(emos) o bloqueto de cobranca com as " +
                          "caracteristicas descritas neste Comprovante de Entrega{br}");
                  mPrinter.drawPageFrame(0, 0, 244, 100, Printer.FILL_BLACK, 2);

                  //REMOVER
                  mPrinter.flush();
                  mPrinter.printPage();
                  //REMOVER

                  mPrinter.setPageRegion(0, 20, 720, 30, Printer.PAGE_LEFT);
                  mPrinter.setPageXY(155, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}{w}{h}RECIBO DO PAGADOR{br}");
                  y = 50;
                  //logo
                  mPrinter.setPageRegion(720, y, 70, 270, Printer.PAGE_TOP);
                  if (logo != null) {
                    if(android.os.Build.VERSION.SDK_INT >= 26){
                      byte[] decodedString = Base64.getDecoder().decode(logo.getBytes("UTF-8"));
                      Bitmap bitm = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                      final int wid = bitm.getWidth();
                      final int heig = bitm.getHeight();
                      final int[] ar = new int[wid * heig];
                      bitm.getPixels(ar, 0, wid, 0, 0, wid, heig);
                      bitm.recycle();

                      mPrinter.printCompressedImage(ar, wid, heig, Printer.ALIGN_LEFT, true);
                    }else{
                      byte[] decodedString = android.util.Base64.decode(logo, android.util.Base64.DEFAULT);
                      Bitmap bitm = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                      final int wid = bitm.getWidth();
                      final int heig = bitm.getHeight();
                      final int[] ar = new int[wid * heig];
                      bitm.getPixels(ar, 0, wid, 0, 0, wid, heig);
                      bitm.recycle();

                      mPrinter.printCompressedImage(ar, wid, heig, Printer.ALIGN_LEFT, true);
                    }
                  }
                  y += 270;
                  //numero banco
                  mPrinter.setPageRegion(718, y, 70, 190, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 18);
                  mPrinter.drawPageFrame(0, 0, 74, 194, Printer.FILL_BLACK, 2);
                  mPrinter.printTaggedText("{reset}{center}{b}{w}{h}" + data.getString("bank_code") + "-" + data.getString("bank_digit") + "{br}");
                  y = 50;
                  //comprovante sacado
                  //pagador
                  mPrinter.setPageRegion(0, y, 720, 63, Printer.PAGE_LEFT);
                  mPrinter.setPageXY(155, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Pagador{br}");
                  mPrinter.drawPageFrame(150, 0, 570, 65, Printer.FILL_BLACK, 2);
                  mPrinter.setPageXY(155, 33);
                  mPrinter.printTaggedText("{reset}{left}{h}{s}" + installment.getString("paying_name") + " - CNPJ: " + installment.getString("document_client") + "{br}");
                  y += 63;
                  //Id documento
                  mPrinter.setPageRegion(657, y, 63, 172, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Id. Documento{br}");
                  mPrinter.drawPageFrame(0, 0, 63, 174, Printer.FILL_BLACK, 2);
                  mPrinter.setPageXY(5, 33);
                  mPrinter.printTaggedText("{reset}{right}{h}{s}" + installment.getString("financial_document") + " {br}");
                  y += 172;
                  //numero documento
                  mPrinter.setPageRegion(657, y, 63, 225, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Numero Documento{br}");
                  mPrinter.drawPageFrame(0, 0, 63, 225, Printer.FILL_BLACK, 2);
                  mPrinter.setPageXY(5, 33);
                  mPrinter.printTaggedText("{reset}{right}{h}{s}" + installment.getString("financial_document") + " {br}");
                  y = 113;
                  //Agencia codigo cedente
                  mPrinter.setPageRegion(594, y, 63, 235, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Agencia / Codigo Cedente{br}");
                  mPrinter.drawPageFrame(0, 0, 65, 237, Printer.FILL_BLACK, 2);
                  mPrinter.setPageXY(5, 33);
                  mPrinter.printTaggedText("{reset}{right}{h}{s}" + data.getString("agency") + "/" + data.getString("number_account") + "-" + installment.getString("recipient_two") + " {br}");
                  y += 235;
                  //Vencimento
                  mPrinter.setPageRegion(594, y, 63, 162, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Vencimento{br}");
                  mPrinter.drawPageFrame(0, 0, 65, 162, Printer.FILL_BLACK, 2);
                  mPrinter.setPageXY(5, 33);
                  mPrinter.printTaggedText("{reset}{right}{h}{s}" + installment.getString("due") + " {br}");
                  y = 113;
                  //Nosso numero
                  mPrinter.setPageRegion(531, y, 63, 397, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Nosso Numero{br}");
                  mPrinter.drawPageFrame(0, 0, 65, 397, Printer.FILL_BLACK, 2);
                  mPrinter.setPageXY(5, 33);
                  mPrinter.printTaggedText("{reset}{right}{h}{s}" + installment.getString("detail_our_number") + "-" + installment.getString("cod_num") + " {br}");
                  y = 113;
                  //Especie
                  mPrinter.setPageRegion(468, y, 63, 100, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Especie{br}");
                  mPrinter.drawPageFrame(0, 0, 65, 102, Printer.FILL_BLACK, 2);
                  mPrinter.setPageXY(5, 33);
                  mPrinter.printTaggedText("{reset}{right}{h}{s} DM {br}");
                  y += 100;
                  //Valor
                  mPrinter.setPageRegion(468, y, 63, 297, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Valor Documento{br}");
                  mPrinter.drawPageFrame(0, 0, 65, 297, Printer.FILL_BLACK, 2);
                  mPrinter.setPageXY(5, 33);
                  mPrinter.printTaggedText("{reset}{right}{h}{s}" + installment.getString("document_value") + " {br}");
                  y = 113;
                  //QR-Code
                  mPrinter.setPageRegion(214, y, 254, 195, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}QR Linha Digitavel{br}");
                  mPrinter.drawPageFrame(0, 0, 256, 197, Printer.FILL_BLACK, 2);
                  mPrinter.setBarcode(Printer.ALIGN_CENTER, true, 2, Printer.HRI_NONE, 170);
                  mPrinter.printQRCode(9, 3, installment.getString("barcode"));
                  mPrinter.feedPaper(38);
                  y += 195;
                  //Desconto
                  mPrinter.setPageRegion(405, y, 63, 202, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}(-) Desc/Abatimentos{br}");
                  mPrinter.drawPageFrame(0, 0, 65, 202, Printer.FILL_BLACK, 2);
                  //Outras deducoes
                  mPrinter.setPageRegion(342, y, 63, 202, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}(-) Outras Deducoes{br}");
                  mPrinter.drawPageFrame(0, 0, 65, 202, Printer.FILL_BLACK, 2);
                  //Multa
                  mPrinter.setPageRegion(279, y, 64, 202, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}(+) Multa/Mora{br}");
                  mPrinter.drawPageFrame(0, 0, 66, 202, Printer.FILL_BLACK, 2);
                  //Outros acrecimos
                  mPrinter.setPageRegion(214, y, 65, 202, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}(+) Outros Acrescimos{br}");
                  mPrinter.drawPageFrame(0, 0, 66, 202, Printer.FILL_BLACK, 2);
                  y = 113;
                  //Valor cobrado
                  mPrinter.setPageRegion(150, y, 65, 397, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}(=) Valor Cobrado{br}");
                  mPrinter.drawPageFrame(0, 0, 68, 397, Printer.FILL_BLACK, 2);

                  mPrinter.setPageRegion(0, 435, 830, 60, Printer.PAGE_LEFT);
                  mPrinter.setPageXY(0, 20);

                  y = 0;

                  //REMOVER
                  mPrinter.flush();
                  mPrinter.printPage();
                  //REMOVER

                  //logo
                  y += 20;
                  mPrinter.setPageRegion(720, y, 70, 270, Printer.PAGE_TOP);
                  if (logo != null) {
                    if(android.os.Build.VERSION.SDK_INT >= 26){
                      byte[] decodedString = Base64.getDecoder().decode(logo.getBytes("UTF-8"));
                      Bitmap bitm = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                      final int wid = bitm.getWidth();
                      final int heig = bitm.getHeight();
                      final int[] ar = new int[wid * heig];
                      bitm.getPixels(ar, 0, wid, 0, 0, wid, heig);
                      bitm.recycle();

                      mPrinter.printCompressedImage(ar, wid, heig, Printer.ALIGN_LEFT, true);
                    }else{
                      byte[] decodedString = android.util.Base64.decode(logo, android.util.Base64.DEFAULT);
                      Bitmap bitm = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                      final int wid = bitm.getWidth();
                      final int heig = bitm.getHeight();
                      final int[] ar = new int[wid * heig];
                      bitm.getPixels(ar, 0, wid, 0, 0, wid, heig);
                      bitm.recycle();

                      mPrinter.printCompressedImage(ar, wid, heig, Printer.ALIGN_LEFT, true);
                    }
                  }
                  //numero banco
                  mPrinter.setPageRegion(718, 300, 70, 190, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 18);
                  mPrinter.drawPageFrame(0, 0, 74, 194, Printer.FILL_BLACK, 2);
                  mPrinter.printTaggedText("{reset}{center}{b}{w}{h}" + data.getString("bank_code") + "-" + data.getString("bank_digit") + "{br}");
                  y = 230;
                  //chave
                  mPrinter.setPageRegion(718, 300 + 190, 70, 1000, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 33);
                  mPrinter.drawPageFrame(0, 0, 74, 1010, Printer.FILL_BLACK, 2);
                  mPrinter.printTaggedText("{reset}{left}{w}{s}{h}" + installment.getString("line") + installment.getString("line_two") + "{br}");

                  //Local pagamento
                  mPrinter.setPageRegion(657, 30, 63, 1150, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Local Pagamento{br}");
                  mPrinter.drawPageFrame(0, 0, 63, 1150, Printer.FILL_BLACK, 2);
                  mPrinter.setPageXY(5, 33);
                  mPrinter.printTaggedText("{reset}{left}{h}{s}ATE O VENCIMENTO, PAGAVEL EM QUALQUER BANCO.{br}");

                  //Vencimento
                  mPrinter.setPageRegion(657, 1178, 63, 320, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Vencimento{br}");
                  mPrinter.drawPageFrame(0, 0, 63, 320, Printer.FILL_BLACK, 2);
                  mPrinter.setPageXY(5, 33);
                  mPrinter.printTaggedText("{reset}{right}{h}{s}" + installment.getString("due") + " {br}");

                  //Beneficiario
                  mPrinter.setPageRegion(596, 30, 63, 1150, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Beneficiario{br}");
                  mPrinter.drawPageFrame(0, 0, 63, 1150, Printer.FILL_BLACK, 2);
                  mPrinter.setPageXY(5, 33);
                  mPrinter.printTaggedText("{reset}{left}{h}{s}" + installment.getString("recipient") + " - CNPJ: " + installment.getString("cnpj") + " {br}");

                  //Agencia / Codigo Cedente
                  mPrinter.setPageRegion(596, 1178, 63, 320, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Agencia / Codigo Cedente{br}");
                  mPrinter.drawPageFrame(0, 0, 63, 320, Printer.FILL_BLACK, 2);
                  mPrinter.setPageXY(5, 33);
                  mPrinter.printTaggedText("{reset}{right}{h}{s}" + data.getString("agency") + "/" + data.getString("number_account") + "-" + installment.getString("recipient_two") + " {br}");

                  //Data do Documento
                  mPrinter.setPageRegion(535, 30, 63, 200, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Data do Documento{br}");
                  mPrinter.drawPageFrame(0, 0, 63, 202, Printer.FILL_BLACK, 2);
                  mPrinter.setPageXY(5, 33);

                  mPrinter.printTaggedText("{reset}{center}{h}{s}" + installment.getString("financial_emission") + " {br}");
                  y = 30 + 200;

                  //Numero do Documento
                  mPrinter.setPageRegion(535, y, 63, 350, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Numero do Documento{br}");
                  mPrinter.drawPageFrame(0, 0, 63, 352, Printer.FILL_BLACK, 2);
                  mPrinter.setPageXY(5, 33);
                  mPrinter.printTaggedText("{reset}{center}{h}{s}" + installment.getString("financial_document") + " {br}");
                  y += 350;

                  //Especie do Documento
                  mPrinter.setPageRegion(535, y, 63, 200, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Especie do Documento{br}");
                  mPrinter.drawPageFrame(0, 0, 63, 202, Printer.FILL_BLACK, 2);
                  mPrinter.setPageXY(5, 33);
                  mPrinter.printTaggedText("{reset}{center}{h}{s} DM {br}");
                  y += 200;

                  //Aceite
                  mPrinter.setPageRegion(535, y, 63, 200, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Aceite{br}");
                  mPrinter.drawPageFrame(0, 0, 63, 202, Printer.FILL_BLACK, 2);
                  mPrinter.setPageXY(5, 33);
                  mPrinter.printTaggedText("{reset}{center}{h}{s} N {br}");
                  y += 200;

                  //Data Processamento
                  mPrinter.setPageRegion(535, y, 63, 200, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Data Processamento{br}");
                  mPrinter.drawPageFrame(0, 0, 63, 202, Printer.FILL_BLACK, 2);
                  mPrinter.setPageXY(5, 33);
                  mPrinter.printTaggedText("{reset}{center}{h}{s}" + installment.getString("current_date") + " {br}");

                  //Nosso Numero
                  mPrinter.setPageRegion(535, 1178, 63, 320, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Nosso Numero{br}");
                  mPrinter.drawPageFrame(0, 0, 63, 320, Printer.FILL_BLACK, 2);
                  mPrinter.setPageXY(5, 33);
                  mPrinter.printTaggedText("{reset}{right}{h}{s}" + installment.getString("detail_our_number") + " {br}");

                  //Uso Banco
                  mPrinter.setPageRegion(472, 30, 65, 250, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Uso do Banco{br}");
                  mPrinter.drawPageFrame(0, 0, 65, 252, Printer.FILL_BLACK, 2);
                  y = 30 + 250;

                  //Especie
                  mPrinter.setPageRegion(472, y, 65, 280, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Especie{br}");
                  mPrinter.drawPageFrame(0, 0, 65, 282, Printer.FILL_BLACK, 2);
                  mPrinter.setPageXY(5, 33);
                  mPrinter.printTaggedText("{reset}{center}{h}{s}REAL {br}");
                  y += 280;

                  //antidade Moeda
                  mPrinter.setPageRegion(472, y, 65, 310, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Qantidade Moeda{br}");
                  mPrinter.drawPageFrame(0, 0, 65, 312, Printer.FILL_BLACK, 2);
                  y += 310;

                  //Vlor Moeda
                  mPrinter.setPageRegion(472, y, 65, 310, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Valor Moeda{br}");
                  mPrinter.drawPageFrame(0, 0, 65, 312, Printer.FILL_BLACK, 2);
                  y += 310;

                  //Valor Documento
                  mPrinter.setPageRegion(472, 1178, 65, 320, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Valor Documento{br}");
                  mPrinter.drawPageFrame(0, 0, 65, 320, Printer.FILL_BLACK, 2);
                  mPrinter.setPageXY(5, 33);
                  mPrinter.printTaggedText("{reset}{right}{h}{s}" + installment.getString("document_value") + " {br}");

                  //Instrucoes
                  mPrinter.setPageRegion(282, 30, 192, 1150, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Instrucoes{br}");
                  mPrinter.drawPageFrame(0, 0, 192, 1150, Printer.FILL_BLACK, 2);
                  mPrinter.setPageXY(5, 33);
                  mPrinter.printTaggedText("{reset}{left}{h}{s}Apos o vencimento, cobrar juros de R$" + installment.getString("day_multa") + " por dia de atraso {br}" + "Apos " + installment.getString("due") + " cobrar multa de R$ " + installment.getString("fine_string") + "{br}" + installment.getString("protest_date") + "{br}");


                  //Descontos / Abatimentos
                  mPrinter.setPageRegion(409, 1178, 65, 320, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}(-)Descontos / Abatimentos{br}");
                  mPrinter.drawPageFrame(0, 0, 65, 320, Printer.FILL_BLACK, 2);

                  //Outras deducoes
                  mPrinter.setPageRegion(348, 1178, 63, 320, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}(-)Outras Deducoes{br}");
                  mPrinter.drawPageFrame(0, 0, 63, 320, Printer.FILL_BLACK, 2);

                  //(+) Mora / Multa
                  mPrinter.setPageRegion(283, 1178, 67, 320, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}(+)Mora / Multa{br}");
                  mPrinter.drawPageFrame(0, 0, 67, 320, Printer.FILL_BLACK, 2);

                  //Pagador
                  mPrinter.setPageRegion(154, 30, 130, 1150, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}Pagador{br}");
                  mPrinter.drawPageFrame(0, 0, 130, 1150, Printer.FILL_BLACK, 2);
                  mPrinter.setPageXY(5, 33);
                  mPrinter.printTaggedText("{reset}{left}{h}{s}" + installment.getString("paying_name") + " - CPF/CNPJ: " + installment.getString("document_client") + " {br}");

                  //(+) Outros acréscimos
                  mPrinter.setPageRegion(219, 1178, 65, 320, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}(+)Outros acrescimos{br}");
                  mPrinter.drawPageFrame(0, 0, 65, 320, Printer.FILL_BLACK, 2);

                  //(=) Valor Cobrado
                  mPrinter.setPageRegion(154, 1178, 67, 320, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}(=)Valor Cobrado{br}");
                  mPrinter.drawPageFrame(0, 0, 67, 320, Printer.FILL_BLACK, 2);

                  mPrinter.setPageRegion(0, 30, 152, 1470, Printer.PAGE_TOP);
                  mPrinter.setPageXY(5, 5);
                  mPrinter.printTaggedText("{reset}{right}{s}Autenticacao Mecanica / Ficha de Compensacao{br}");
                  mPrinter.setPageXY(30, 5);
                  mPrinter.setBarcode(Printer.ALIGN_LEFT, false, 3, Printer.HRI_NONE, 100);
                  if (installment.getString("barcode") != null) {
                    mPrinter.printBarcode(Printer.BARCODE_ITF, installment.getString("barcode"));
                  }

                  //REMOVER
                  mPrinter.flush();
                  mPrinter.printPage();
                  //REMOVER
                  mPrinter.feedPaper(110);

              }
            } catch (JSONException e) {
              e.printStackTrace();
            }

          }else{
            mPrinter.printTaggedText(args.get(i));
          }
        }
        mPrinter.flush();
        result.success(true);
      } catch (IOException e) {
        result.success(false);
      } catch (NullPointerException e){
        result.success(false);
      }
    }else {
      result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  public boolean connect(String address) throws IOException {
    try{
      BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
      BluetoothDevice device = adapter.getRemoteDevice(address);
      mmDevice = device;
      UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
      mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
      mmSocket.connect();
      mmOutputStream = mmSocket.getOutputStream();
      mmInputStream = mmSocket.getInputStream();
      initializePrinter(mmInputStream, mmOutputStream);
      return true;
    }catch(Exception error){
      throw error;
    }
  }

  protected void initializePrinter(InputStream inputStream, OutputStream outputStream) throws IOException {
    mProtocolAdapter = new ProtocolAdapter(inputStream, outputStream);
    if (mProtocolAdapter.isProtocolEnabled()) {
      final ProtocolAdapter.Channel channel = mProtocolAdapter.getChannel(ProtocolAdapter.CHANNEL_PRINTER);

      // Create new event pulling thread
      new Thread(new Runnable() {
        @Override
        public void run() {
          while (true) {
            try {
              Thread.sleep(50);
            } catch (InterruptedException e) {
            }
            try {
              channel.pullEvent();
            } catch (IOException e) {

              break;
            }
          }
        }
      }).start();
      mPrinter = new Printer(channel.getInputStream(), channel.getOutputStream());
    } else {
      mPrinter = new Printer(mProtocolAdapter.getRawInputStream(), mProtocolAdapter.getRawOutputStream());
    }

  }

  public void disconnect() throws IOException{
    try {
      mmSocket.close();

      if (mPrinter != null) {
        mPrinter.release();
      }

      if (mProtocolAdapter != null) {
        mProtocolAdapter.release();
      }


    } catch (Exception e) {

    }
  }

}
