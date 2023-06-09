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
            // String jsonString = "{\"data\":{\"id\":\"0018cb00-cc97-11ed-8bce-0242ac1c0002\",\"company_group_id\":\"5e7c3ea1-bbf8-4254-8d7d-8b730d7548ac\",\"created_at\":\"2023-03-27T00:00:00.000000Z\",\"updated_at\":\"2023-03-29T15:59:37.000000Z\",\"ref\":\"495200\",\"xml\":true,\"serie\":\"20\",\"numero\":\"1424\",\"chave\":\"41230508168798000197550200000014241414369506\",\"protocolo\":\"141230000287242\",\"natOp\":\"VENDA DE MERCADORIA\",\"dhEmi\":\"06/05/2023\",\"dSaiEnt\":\"--/--/--\",\"hSaiEnt\":\"--:--\",\"bcIcms\":\"\",\"vIcms\":\"\",\"bcIcmsSt\":\"\",\"vIcmsSt\":\"\",\"ttProdutos\":\"217.60\",\"vFrete\":\"0.00\",\"vSeguro\":\"0.00\",\"vDesc\":\"0.00\",\"vOutro\":\"0.00\",\"vIPI\":\"0.00\",\"ttNota\":\"217.60\",\"adicionais\":\"NF Ref. Pedido No. 0000002285\",\"emitNome\":\"NOME DA EMPRESA FANTASIA\",\"emitEnd\":\"RUA GETULIO VARGASS, 195 Centro CEP: 85010-280 GUARAPUAVA-PR\",\"emitCNPJ\":\"08.168.798/0001-97\",\"emitIe\":\"9041463667\",\"emitTel\":\"42369-5878\",\"destRazao\":\"NF-E EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL\",\"destEnd\":\"RUA CORONEL SALDANHA, 3 CENTRO CEP: 85010-130 GUARAPUAVA-PR\",\"destDocument\":\"713.867.779-00\",\"destIe\":\"9\",\"produtos\":[{\"cProd\":\"100260\",\"cEAN\":\"7896016608766\",\"xProd\":\"NOTA FISCAL EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL\",\"NCM\":\"20098990\",\"CEST\":\"1701100\",\"cBenef\":\"PR830001\",\"CFOP\":\"5102\",\"uCom\":\"UN\",\"qCom\":\"5.0000\",\"vUnCom\":\"7.4900000000\",\"vProd\":\"37.45\",\"cEANTrib\":\"7896016608766\",\"uTrib\":\"UN\",\"qTrib\":\"5.0000\",\"vUnTrib\":\"7.4900000000\",\"indTot\":\"1\",\"nItem\":\"10\",\"CST\":\"-/-\"}],\"faturas\":[{\"nDup\":\"001\",\"dVenc\":\"2023-05-06\",\"vDup\":\"217.60\"}]}}";

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
            String jsonString = "{\"installments\":[{\"installment\":3,\"line\":\"34191.09008 00805.573854\",\"line_two\":\"75866.750005 1 80460000250000\",\"paying_name\":\"BANCO DO BRASIL S.A X teste 1\",\"document_client\":\"00.000.000/0001-91\",\"recipient\":\"NOME DA EMPRESA FANTASIA\",\"cnpj\":\"08.168.798/0001-97\",\"due\":\"18/10/2019\",\"current_date\":\"09/06/2023\",\"recipient_two\":\"5\",\"document_value\":\"R$ 2.500,00\",\"day_multa\":\"0.75\",\"fine_string\":\"50,00\",\"protest_date\":\"Protestar em 20/10/2019\",\"cod_num\":\"7\",\"financial_emission\":\"27/09/2019\",\"financial_document\":\"0000001043-003\",\"detail_our_number\":\"00008055\",\"barcode\":\"34191804600002500001090000805573857586675000\"},{\"installment\":2,\"line\":\"34191.09008 00805.403854\",\"line_two\":\"75866.750005 5 80390000250000\",\"paying_name\":\"BANCO DO BRASIL S.A X teste 1\",\"document_client\":\"00.000.000/0001-91\",\"recipient\":\"NOME DA EMPRESA FANTASIA\",\"cnpj\":\"08.168.798/0001-97\",\"due\":\"11/10/2019\",\"current_date\":\"09/06/2023\",\"recipient_two\":\"5\",\"document_value\":\"R$ 2.500,00\",\"day_multa\":\"0.75\",\"fine_string\":\"50,00\",\"protest_date\":\"Protestar em 13/10/2019\",\"cod_num\":\"0\",\"financial_emission\":\"27/09/2019\",\"financial_document\":\"0000001043-002\",\"detail_our_number\":\"00008054\",\"barcode\":\"34195803900002500001090000805403857586675000\"},{\"installment\":1,\"line\":\"34191.09008 00805.323854\",\"line_two\":\"75866.750005 1 80320000250000\",\"paying_name\":\"BANCO DO BRASIL S.A X teste 1\",\"document_client\":\"00.000.000/0001-91\",\"recipient\":\"NOME DA EMPRESA FANTASIA\",\"cnpj\":\"08.168.798/0001-97\",\"due\":\"04/10/2019\",\"current_date\":\"09/06/2023\",\"recipient_two\":\"5\",\"document_value\":\"R$ 2.500,00\",\"day_multa\":\"0.75\",\"fine_string\":\"50,00\",\"protest_date\":\"Protestar em 06/10/2019\",\"cod_num\":\"2\",\"financial_emission\":\"27/09/2019\",\"financial_document\":\"0000001043-001\",\"detail_our_number\":\"00008053\",\"barcode\":\"34191803200002500001090000805323857586675000\"}],\"number_account\":null,\"agency\":null,\"agency_dig\":null,\"bank_code\":\"341\",\"bank_digit\":\"7\",\"bank_logo\":\"data:image/webp;base64,UklGRpIeAABXRUJQVlA4TIUeAAAvp8AQADWHmgBIGOhfG9z+ETEBKKbM8A8U1dp5/291ZKfbtX187ONzPPaMZ3a0nmUYvMzMzDizmr3MzDh7w8x4mWm0MFqH5jIzM45Go5VGlmUdHT16pEePngv2Bh1+AdHtwlwxMzOdYM9QMTNNF5oXcE+YS8ZylZo5Tsuct8DMU94uzG200IWZOZ1D7b9wmPEN3DJc4SWXYWbmOrpVGCtGUbS2vW3b/AeirOx5/5fTXWdok+IGF0AsEgTBJRzatm07jaNTB7g5X0ChXB2e/N7//y85OaoxkuDC5bj/DNw2UuRjXj76gd1n/1dHdopZ+/c7v+/3ezyeZYahMDMzMzMzZzVhZmZmGIWZmZk5I8saWdbIGmk0sqyRZVmyrKOxz8AFbx/mdNGls0rNKbcOw7Yupgxzqu3CzPCTRtMFp02VijkpHWmrVJxU92/gdqELc6pLVThHynTRxTLMjA5UJ/QXYHWxDDO7mG6VijvsLoX50ijlFhStbW/bNjBDbSmKu/ee9389e7calETg/77v/wGYAgCAZSSp37E79kyNoGlTjFZv27Zt27Zt27Zt28Z6d+6+/w7cRlIkZ8V7zPACBrBdoR0FWK5Lp9PRgY2MwR/8T+uMVplL17Ma52cAgAJF0/yb3hROS4OJDSZmumBnQ87Uv1BlwpW5733Dzow7bkRLIo1pbGxMYBSQwq7VfyNtOtyntLQ0PSYVSkw4hSKJjQ5Mggr9TPsZ3kPVyGhpOn3UmtmmquvNzXXetbWbvXY6a6dds8eune6znvXYtcF9xzueFSt2TD9fZ9bWmdV1mrTeG7+eU21DU2EPU9z1iKe19Euoz0Q705igGJSfESgeVNMnunPLnmUI0d4Ue1VYAOaQOagt5siyTHtAw7AHMk3TiMXcYK4xl2eemYXIQ3RIXOvc8qW58Ks/g30YToEEHTQG05nVPZ0yKhsGsKPCBxIEJEACqCMiUAFiEeHznz969GgVkEBVs1TDqqFxHMUjdBQ5tip0I39s5t/+6FtrgUnZ/mawbHL8GcRz18YhgwhMsICmUEd4v5aEd0GePuFerLZYIAdkgTAQTzQnAiAt0ICF1Rl6ac1cL5TYgFSMEMqwofYWZ9+zilCdOQkmkiIdoDMRAs1Zsy8D0S1u8WX9OYg3J8JB0GEIG2hn/oQgQIvo2xpc4b2h9WFwnGNbVQPQb6sGJr3lU8pe6yirbdo/IaykaWWQdoORimfITKxUKtVgZHQlpS5rIpAp3AZvHx5VLQ3xdg9EP4gMk5rmF1Y0Lp5gAr2GP6WkZVnSkDWyLCsppTRk0opX27kh2PbQ1wqalmGCABReRKLD3+VcaE8CMMggwv1svUgdWTuQPblUi8UcLNnUi2K4U8AAFO1sS0kfOqQKEgQEEgRF2FLKOKvm8enTnUodEWwuVFiglibJPP02ILn6MFtyNaBZl9qy6oMLFhaUqFx16HClfiNL3wCGoPXlMVmBmY8i6xzAgFTWUOf1YkhZC/0D5U9cRh47WppFeyoOVEiMINWjMaS2Eh4By2nHFJZlybh8+K14VwNS60unApDL/KkplawnWScpCdacefRbEaJANewySQTNWNKo+4jqeboEe85c+K3NvhLgMmOWXPoJbdqMlRoAjgTJzAhsXSD55dgRGgbQTSavbhhG3ZKxLMvKzJeplkuvRUE1ILXETPFQILkAwpxivmF/kgi7eZxksyENWa8ddOZr4tJz6T5KQVugErRiU8u0gKYlpZzINCeS9QlBhw1L6RFoIhO0k/OBfAT6cjMQTWQmx0GoQMsEgkw9DpBqbn5uE5IgzWnmq3NEAQgABih69G/VgBAZgi0uH06lyJJTACxFPa4uQCFrqcUgW0Gwz6InQBWOAcfPSaY0NXwjJS2DYBvS0Ge1ASdUqwhShYYAEeioWUB0gPSJzEEuXD2CcHUCxy+fBgAMiuJ9kNFKVwhTBJGtnY8tWwY3F6YSJGSGEOyQMx2Z6qREpxwRwrcAmh0s2qFFOzLcdY4QCBWZoCIowkYSQ9b6Zj0151OpqCDoYKYT0p1KsuucRrZBYyDf5AI1RnjX7A9tQC0xC/UTPbifh7peXVfPDMgvX8izvEfbGqJtGSOHy4IumUUuHvTnPcuz4DN+7/8dDmG+nVV+M/9d7evKCb6dOGZd3dFFxd2GORGmRFpJK5WygCR+SlZyHEt+DkLeQz3RvUT+SV5jf569IJDfRtb7cg3FsjehhZQI28+1SQfjkEotQTf5lCwVXhkega7kSyOA+wv7nEkDNRzWlhfen+YTDoCwT6f6lNJwuATAN6T+wVve/rCDj3/8Gb59eNfIwceL1C22MO1ZlAaOnVeoeLdU8XbpgHkrJVQ0k7XxzeI6LbEn+971/C06vYwGgN35zKYG4zIEY+Y557PMicxMZljbYO6ttc8P/TVSIusuj7IqCo2qRUIYqB6XmgtPR0Vuk18+PaibyOqtjWrfiXJMj+3oV9MKVq1xRmd6hg8GBOB/XGMAAI05algGhRryYu0XvOID+AdUA+rANyR4R+DTXQ/ULxEE4NMds6HttsuYnCIp4Zkk6TB1A9/b9fL3qvubtifUn8tqr//5mtAiUfqhdAq6ajTBTKT/ajTxLCAX0FPocDe5e4p0BAL1v4RUEPjqwjXEykjvnAtn0H+QohFez/wUC5mKBehQ9u+B7sVfB6nbNY08UXb0oenvYO5tVHsRVF5P+xMAdMW6cRgHGVqqC4UJ9kfdskQP9RXfLBe5jMmA8tsHApqFU+2onaHb0WFaZNpcsdh82c6jkRnyukVB5EZshuaRE+Nc7lk8j8TJGzOHUUQXvPyZv3zLqToXf+Yw7cKQm1NuLj2cIbS53d90nRfiLKdops9WF1cFAzEoHo8IIHZPtKfGs3CoH3WbzUVBXpmDpd4j7re8AWoP0tWywvpAh0qeC3Nl2g7WrtywxA4rzThKe2pU3nP3G4Zkld1GEdZXej3ZkZU3a69V7Yqx/IGXqmxE7HFMzUc02IuWMPh+JXyQQ+IP4+tX7gv+51xx3mkSlM9rZPPlQk3BylI8mXdyhgioINSighHU1kZ55XMQ6hXBhfdYrrIdjVNU494HLqsaANqKY2xxETlROcVRz0dpkfirrzilVlB55YUXjVOtBU6JNpeSy46O5kU1NOTFcSbqJpeUByahcC75Hj6x9pHSKwY7Y7YwmNSPw8BZo7fc1Du78HlFi8Y7oXsSVnqrmy8lrPWqTL2RhBWV7aIYjiAvNY9hecx1k2iPcUh/WO0T6aE7wEZrzg/SyoQoAmcEr5WgQjzJdR8Z7SdmaubF6a2SuLIT5yC6h1zgHgq9A7k7mYFqrE6NcRBWbmxcbupu5IkYfBq9RFPPzF07ekoU/ltsUe3jV0mEoGE0nHEWqcthNk4sQx405Y2sTRwF5Xjd89oHz5+I23mOSFdAiL7kpzUGREnCYer6ldvC0OK6E2vSwix2qrz66V5jX6pXTsXSrXy44/ruq7/E/Ddzx1m4lfg7u9ifXOrA90HT84UY2GW9Vd1DOyIXf3JvkLlTrPb6nie5R59Xc58HhWXWHqqgyQR6TeGT/LTGcetKp6qD7fx0LQ8JtNwUcRRQK/FAxbvaLlaCipUyvdFARtBBAmyqSa23MozHhplyZHXXMPiFmTsjisFBhoKEo+1fSue0MHEmtxoGWOs5GYdkZaMoLnobs+4RPB2rmF45kzpNFdS4eVPfC6qPbyyMlWtTH2z0JPF3bfip5WSMYhe9DWnxgJaq9hJcVrbIgdcni3zVW1vj7MD9pqBtY5pScs/rHFOfnhPdusUieMMEfmNl4YsbqV8Y1E6ipLeDlb1ZZLCzaGuTnQxfjKFuHg7QbpnuG1P3ohiiaYhi4eec0cmpQQTxgfz886IuY3KKrISX+eN7/Ovc0P31HV6z+PhVSwH3F4r7bdV74C/2Wi2xS3r6rV2j3fISD52/apUNbRK+j5r+55LvWD97haRC/MnC4mqv2Z8KrxLuoxIZY/nxL3ed4AKMLqh6adzhr7v97whWZeSYQrXaKy5RzICwa7yIYLig6ew09vZq2gqa5iPZ4avpLOAj43aw13MLpQT32uKJmJtLEAAMFrCgxHtH6wslKu9/ZfMtKbN661rEFmpduaC0nQRRJFG87str0A2qbPNqGGNHOYPMtDMO0cpO5Ur9mHEYmvr0MlFFN3lAG8lg6n1crRdLnqGavzyvbbnUVUmElfVW9s6iwrSRJOjwel/YNjeXKV5niZ8d9YXXrvtIwybjF14PBx9LYD4jOzUSvxjG1F0alFGO5yehS3Zy5YzPavRSuN1DdJMFJfv3QRlrVslEoWz09RgTXnkIARSHlnfvtIszOUVX3AZC1ZqHxzqBAzf0oGULxSt5jO0r7Xfsvry/53zPjbJHey/rA/ZXDlrE/Df3Mdbwturle9ofviK9uOTzJ4s/fbPa+flT9H7JFoRfRYuxk/LU4jLXcX40mkIeF0KLZMFtlouQxgS1TlEvu0ru95VoCmzXz+BhEEzmMI/+RQnaybyj/aq4jVosGKHdpivbqFgaomky9VI3qauWWlhLiUmNUzuRnJ6/gBfgIa+TEcKQMX+hKN45SGxQATM3ZktiqgyjayHxUOuW3vRzcxdimitnzOCBYjGUw8Iw6g6xhH6O3DgEizyVwxEmKIWgvMSzre03EqVxgrZSmgyjbTP0rKrUQXoc/5eQxbH+llbubfQHzuQW8zPj4EGzmpp5zsRGghos8mJjcTEKYxIelLiqW2B20n7JRi+CMrUjzqCeyVk7UYycOQ8bnrJz+deodhSUnS2pe2/wWJ04SHw0Dn7YMIEMlcB+m5vq5RRNJ3Dk9s/twWvCR3krbXR1Q2QMB0buNA/aGYu8QpKoZkkIO2uFN6ddk7LacgqL7Xp19enZSITKe9WvSo/gbKGu8/6QllhPEoK7TOFtahhxBCB2Za4sC95jW0vLbJ+n8dAJJAbvCE3b4V+XW4o0wrujPOscurue+vt7mOAfsaFSdRK0yVgP+48XuqZnP5t7MdQrBxmIrCADwJBR91dRXB00FtCOrtMlklJlGH0LSYBa/sBrE1uN1bLe3A0WGeOJzL9RpdTtoaNU+bOy/KFgUFAXU3928LSetoFeHuFUMSTkKegmtIaxF8Xbt8f5zQm6sa608PAvrByenUUQRTQOppuA5/y0vI4WqV0VZhfsInZylbhxhYK56l1E1UJZmV1prdFl9FzpJmNlvUXamTL4SoOoOnG8PK9dzcSD4qo/F6eqR0CgWuvdgSN3l/1zB3ARZCksbGR1e6SFPUvhJMtulymkiAg5aKZqEvO8swjLnT7e9Hj7Xm77HSxve8TjamXPTa+3g5v0cv7/Lqtp6+BjWmJLRylwLBZO01zkRWBiV2Q9o+J9u13t+nEfxOOTrvtSPbZC1ZRhC7OdhLnTessGuQHAbJ/exzXGmR19vmpz5znjRrgzUBx5EcKQUft2UZwd1Of1kqCJeHtipo67rKzoLWhr3brOvU2GqNbvJvMSksNBb0MnkYB+jsyHoJzEByu7fjieBEdjReH2Igm1CKaaZ28csqNJnnZi8ZeEXItiMHe5XCndhMIthOGlLV2HUX4q46Aknm1Mj/hHkPho6k02+143bQ1jsNGrJLQrFuB5wRnKPTeds+XudrDBSnIm2Oh4zZlKYrA0qGp8lQ9tHFxvefAX7Uuf4uKC0zTFKZCoqMcW9qcdwQWlrYQlv5HN3dEIlvXm8d6MdnpElNZDIcAK9rbycvH2cTcraSn47W//NToE3Fu1opT8z4Xl/fN2VgNrTs//1b1VHlFwn2KOiWNgSlfU3GvgWQv/3H6cwqpBtFmgPHpPObN3W6D/rMnMGlji+50V0MEPq7e2zcjCEkeq2njdpDLi3FUsviTxSzSV83WMALgdwGDZC6+tKE4O6guqTnGb4jAxc4YxtJhEqBW9nfSaM8Si59qYemUn2UFifDmu1MlDQd8XyetgkWO5Ii+nkSSGKR+yYOtmwVCmUY04M2Q1zUAnvT877XsI0yD30NLlZJHBTpJhlAPfOaTmSW9z1SwqNSbU3txliYdyuW1k6m1tCpOXnXFwVnaRhNv8r5bMu7xseCEGCz6ssM5YcsZEN4NFyhnK79Q9CprqLGJUOxoH22cSlkPgCMV30dKky4NOkxSnQN+BwhHLOoE1V/FuaX/YAZwX0Qcrb3y8VUts+aZ2vLeW73QPsjZC7DHXVpTfi4zR8k21ciwcxtfdYL57leXpzY/nB3ZZ79gO7lJLv310hjU8vRBeRUveGNEhVMh/WGiiLoel3rVwrhH6ev2L262nsKY/hoWZD5rG8FS8jTlIS/jAx1Yt2P+P3TqmXy8SS8K0yVYJn94eQ9Qck8pn3FkNgopvX09kodAeMELB3r5DUYb5YgCoVgbDmFqOE9S6ZV6PFjnkliV/BKvQEk+iyFFtf+6OQgd9BJ7XySL7hGqEiKCClSVJ8PxqBaiac1H4f/bfqiF/C+OQDq2bZKnFccaaO/lji8xxlVzKZ2nFWW9TlcTCCH7kdYipoyiMKK7mzk1O+NRJZ+q1nVx4HTd+coDeogq/hXHuHa+VnfSCElPfEJbAGF/am/PaDBkEUBx8iQJQQN37tGqK20D7lYfH+u+rfLtQrF1EaEJUUWsz5drwKlb+XD3FsttpDgMQiCevHa8s5/ug6Z/zu7keTa3Mop/VzR6LQKgnS/ZG4Jhbav4sqytrrq9qiZ0pOoQX//iGue8ovADuqrh3qZUfqtEhsF2dfn2Cxx1Vw7DKsEJKYhKKi6dmwGCBzpKzbmoG+JPH0kvp2Yxw5MxNDKb1Bup0gkrr/IfAawjGizlnZHRzEkWf2JyHS/M6A+es0sotxxqywszrfKmcmCAjCBex0bcDfrbDxEOfvjL1XhSxXNlD5GsXtXKbdcobz3Lm4xzFkqMa77nJoxrmH8WZbKfXmr/t4Kc88Cl0eRlMenDUr4Lyvf7I7fg/XT8x0iG5xnKl3Utc4e3k7cJjEoUyHlvri16qWh5G92Vuiq4EcpKmNtayxvyqtTH5z/926qupcdvnVQvKvHfFzT8sRYgBNAA9Z5GFCBCrLB3ON6zcF3wf1Yu1K3gLSe0Vbfrfq0Kb1Am+GAR7vl8d2gKytmKHmvE6KPkU/HUJ9l275mTPRrumLd/fjfaQK2m/Wtr0dVbzVsWvQ6uE67UWOGS3Nl3KAtpDElTXW+4P1oy386FNfLt2NquI4HChfDhpAovbAkV75t2WnnOlImQGaqBMtFqbrH9kYipiG5N+OXeaa2rBDeR5KYtEvzuaN+MQJz5TBI0V7za3xCqV1E1CUJEzWW/l9Xl9PXBqzBjAcEn/dVOf658bgkoDwWN51Nss8V5vXZt5how720OwTNPDeIU0DsEi04U5469/fVN/0dtYrgqI8wy/Kz17O+09WQHmAFpJ7TN4HW3U2ttX4+DL55zNcJdxopYz/HHmbrziRrID+j9ywyhfaurt20WhJa6CRqNr8xYy0m4XgjpdU+LaIt2tdZNtN7PMAoLq9zw0ssVFNpuq4hCBVeOOeFs7sMz6t7n9pW1FNAnU1siq8FpkiJ5uQFVD+oBpq6K/ruupFvXc6azrOQq+9xrbwZk5YkRkCmLuJPeBf8z5bvVwjy3jn/dsAQt9vnKuuNeK6109sCzu4nHEGl+e2D5O9teSt7dL13JkDG8/n6o4T/CZg3wjrRieeOyU9/N8tbDxxBlvkV5QzjhYSZjM8zWe+CpEsgKg8JOB3F+om7xTeNmA/JZhBxgqnXGhRNRIVCqWKRFPTrqqWFXFqVzjukmAzbChEhOIrVTlqbMWH+7oUHOxTqtsSuScVGcQCSOElFCxqqlqK1PMUF2ClSqVmHbVsyYx5YkZS41Zdax6CtR8J9xHP6I6W5ZSqmkHJAbcFhDCKTNWkYrKVFWqZIUmlGt0m2mHBpRRZZJGuEHVqqtcqooF1j0/lPdV4TSkL6qVMb452dggiRxNsp0EScrNkha9/SKbx+AXU7YJVp4sMn6/THXq1tfudlcOG94D+HNn1iFVEQKNOdA4RGxa9nYzcOT2st9bxMD5KLFwurZk8MRV3Eun+JJ6vhC0FFjB9I49SkW6XUvs6T5EFcvKCfpyh1huHDhWw6vYCX4ceNZC1S6zlMcP9rq8cHGnv51y/lO9Z7mo+cdC1YH79pF3a+1vu2XZR82Kdys7GE5Roo70mMv5Zg7GcCesRYf7TSf/5eLIdWVe9xaZeC0qVXKOP1yrKavWwKSXsgjeshMWzbOk+lxfauR5EADqgKwAAHRFtsEqzBFddwzk/rAgd6N0wT0na8G7G3Ybc7CKglp0QO/HWvhpVw5EbMFLQ6F4vYf3PVG03JQioIgYED40jUFFMCG3MluJBYQA7wAD4F1TwnhhbaFzIajg3YaAPB6GqYZVGMADlQOXnSQI8EbAulKgYD6yLYDXhcp2vlLNV5KytcQ73C8EhS1sngkXZzMwlk/bVcjzgVAla9sQbmNfIgSRjsri0a/rRxGKKrz/aR+15LIfkuCo4uUVn96ouAhhj/9KS7Hlnz9mUjsouavh+AhS2sxGU1BLwSPH2iseX/H9zocu6fAqtcT5Tx4yFd1hmb38u3dwzAd36dAiEbxkT/Lncj1fpjeYHtR6WEvhnT5O5VEjjnGBAsMtBGYikc0Pz/YBwXMpQcPWrEbq+rv5cW7gJZvigBkYs4BMQt20e4v8pXusFjsr1k1HiWjaPmRv/SRdH3QtdF0IFLruaFjJ/CDrTN1EXaFuR+5O9IzHreg4ikcINrzc8fSNPAverw+m7oGXD2i8Ybfej8yPNddEy+q1C1oqWja8bY0/4v2osaMi7f9o/yRmpvO8fWY/4eWmjB/Ic9foCmRGAi1CnfdF4F1HDSoLKm4lMD2GwfJpBrdXfJo1M+kOhLjHI+VQP03RA9DbzeMhWUEZidq9IVyZdUgRBAgVYPZ6cL1zvO/Mdi9JhAgcQXhkur3irh5d4fmjdf923wpun3XMJcBACUDMLubxz5r/fXxVeHtxmnKWmCDuMyX6Tv/3eIk7c+tliC7zFZDpQixZRf587bdnFv/4x2lpFqBzVnqVcA8Eu90jZFC1Cea4GoWpiOlK1qPwAxwSAAC7D164uREBuD8Y4mGIopvHzRQIAAjaMVQ679QWapa//IWYndiDaqIa6Ux4T2hfSbcrGxKEJ8Ku/oXpha5EDVAKyBF5LsROVC+ELTJqi+5xj3sEg3QGsiJ5InbNHagjBEAAvoR3dIlfUNhPFsQL4aXijb6kNd4ZnughKyhQT4Z3zY1Jh2jhiqdXIPPyVyJnKmc6P7DbPojH5m60A2VEtN+291B8qP7PveHFfxWa7EwsRBAEQBTp3B3ulDtAQKSJtIEU6PJALCB3gYUqRExQlwwqEljILOosdEU+mcRd5dK4WwBMZgG7JsLgUhS2uHgWmhAmUEGdu86dKHLJbLU4zMnis/h2gpaHMwcq0BXJCh+/AoGDzQub64oX+9IDSxSAL7EZVAAA9T2rvP71r1/uX3IEKIITrVIS0V7VOBL5kxppTBCUQORPEPHpSlS20Qc0AFjlPcsH5E++j8kRjaCjWFXDx4yoaqQxc/USB4Cw+gEIqZHG4NeF0s9a+ntxsO2hEkyg+SEkQdqx33sQlkwSPEKHMELoJYGWORHBIAC1GP8CeZmZCYhAkA7QM00TiQmQgG9htDWh7FsgaDNy1FbVDV3LadmjEAReNyiUprQ+uf0s2KtEsIAmEM/XDHQuECN/c+MUwrEsa762q6M5wyxzoqsD/asPQxhAdAFg583PjZ2BaZ/w3GaQK9wjtY47RV7qRRSI399CMtIy3NDwpcLwC3NQPUyDwA8FwqQhkRaIgiwkQsIYEwFB6PcgPJE13wUkAk2IMKVMNafGeS8ZIMNJKYmWrUmrvmtvrxrZLwUieP8zKOxTbkO2JC7gMAquowASII0JY335y+9FgIvrus+cyaQkQze+8cXf+94pppDDEB6ZR0YBBGgayIE9jJ7R9PXY9V4coD+qNewgdIhodC6irba4AwB1ZDd79sicaaed9kr6fjYE0Rk0BNEbMmTIoEGDyNwr3f1KZOQBv1GL5MjylYArsBi1tfYi7wX3I/iJByXqXxS0kkdp6V+3Rdc0805BxdEcb5nDXrXrX38OfSx0FoCFMfi8v//97887ePB542fDXsltfm+vwB6Qhy5c7kKw2VmMlslI+TdYmcZ0oHah354vMnyVdg354l4uiuJmPV80cf0vXNEpfc0tnDNbOHPxrnjXLVwKW7jkHreFC7eFS+YnP7nq46561fgZ2cI5V3Sp9J/4/pPYf6pt61Y5xK3oPw3TVbtE1RCOxtLEBVrjqKrEan1aS8Mx6Vft4MC8eFRjOh3Q/f/DfW7u2cKJebG+UGIHDrz+i+tqoK7rj2psbEz0f/vcuvHtA8q/T8G2IzsAAA==\"}";

            try {
              JSONObject jsonObject = new JSONObject(jsonString);
              JSONObject data = jsonObject;
              JSONArray installmentsArray = data.getJSONArray("installments");

              mPrinter.reset();
              for (int j = 0; j < installmentsArray.length(); j++) {
                JSONObject installment = installmentsArray.getJSONObject(j);

                 mPrinter.selectPageMode();
                  int y = 0;
                  //logo
                  mPrinter.setPageRegion(50, 0, 270, 70, Printer.PAGE_LEFT);

                  if (data.getString("bank_logo") != null) {
                    if(android.os.Build.VERSION.SDK_INT >= 26){
                      byte[] decodedString = Base64.getDecoder().decode(data.getString("bank_logo").getBytes("UTF-8"));
                      Bitmap bitm = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                      final int wid = bitm.getWidth();
                      final int heig = bitm.getHeight();
                      final int[] ar = new int[wid * heig];
                      bitm.getPixels(ar, 0, width, 0, 0, width, height);
                      bitm.recycle();

                      mPrinter.printCompressedImage(ar, wid, heig, Printer.ALIGN_LEFT, true);
                    }else{
                      byte[] decodedString = android.util.Base64.decode(data.getString("bank_logo"), android.util.Base64.DEFAULT);
                      Bitmap bitm = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                      final int wid = bitm.getWidth();
                      final int heig = bitm.getHeight();
                      final int[] ar = new int[wid * heig];
                      bitm.getPixels(ar, 0, width, 0, 0, width, height);
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

                  mPrinter.printPage();
                  mPrinter.flush();
                  mPrinter.selectStandardMode();

                  mPrinter.selectPageMode();

                  mPrinter.setPageRegion(0, 20, 720, 30, Printer.PAGE_LEFT);
                  mPrinter.setPageXY(155, 5);
                  mPrinter.printTaggedText("{reset}{left}{s}{w}{h}RECIBO DO PAGADOR{br}");
                  y = 50;
                  //logo
                  mPrinter.setPageRegion(720, y, 70, 270, Printer.PAGE_TOP);
                  if (data.getString("bank_logo") != null) {
                    if(android.os.Build.VERSION.SDK_INT >= 26){
                      byte[] decodedString = Base64.getDecoder().decode(data.getString("bank_logo").getBytes("UTF-8"));
                      Bitmap bitm = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                      final int wid = bitm.getWidth();
                      final int heig = bitm.getHeight();
                      final int[] ar = new int[wid * heig];
                      bitm.getPixels(ar, 0, width, 0, 0, width, height);
                      bitm.recycle();

                      mPrinter.printCompressedImage(ar, wid, heig, Printer.ALIGN_LEFT, true);
                    }else{
                      byte[] decodedString = android.util.Base64.decode(data.getString("bank_logo"), android.util.Base64.DEFAULT);
                      Bitmap bitm = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                      final int wid = bitm.getWidth();
                      final int heig = bitm.getHeight();
                      final int[] ar = new int[wid * heig];
                      bitm.getPixels(ar, 0, width, 0, 0, width, height);
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
                  String numberAccount = "";
                  if (accountRequest != null)
                      numberAccount = accountRequest.getNumberAccount();
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

                  mPrinter.printPage();
                  mPrinter.flush();
                  mPrinter.selectStandardMode();

                  mPrinter.selectPageMode();

                  //logo
                  y += 20;
                  mPrinter.setPageRegion(720, y, 70, 270, Printer.PAGE_TOP);
                  if (data.getString("bank_logo") != null) {
                    if(android.os.Build.VERSION.SDK_INT >= 26){
                      byte[] decodedString = Base64.getDecoder().decode(data.getString("bank_logo").getBytes("UTF-8"));
                      Bitmap bitm = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                      final int wid = bitm.getWidth();
                      final int heig = bitm.getHeight();
                      final int[] ar = new int[wid * heig];
                      bitm.getPixels(ar, 0, width, 0, 0, width, height);
                      bitm.recycle();

                      mPrinter.printCompressedImage(ar, wid, heig, Printer.ALIGN_LEFT, true);
                    }else{
                      byte[] decodedString = android.util.Base64.decode(data.getString("bank_logo"), android.util.Base64.DEFAULT);
                      Bitmap bitm = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                      final int wid = bitm.getWidth();
                      final int heig = bitm.getHeight();
                      final int[] ar = new int[wid * heig];
                      bitm.getPixels(ar, 0, width, 0, 0, width, height);
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
                  mPrinter.printTaggedText("{reset}{left}{h}{s}ATE O VENCIMENTO, PAGAVEL PREFERENCIALMENTE NO BANCO " + bankLaunchBillet.getBankAbrev() + "{br}");

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

                  mPrinter.printPage();
                  mPrinter.selectStandardMode();
                  mPrinter.feedPaper(110);
                  mPrinter.flush();

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
