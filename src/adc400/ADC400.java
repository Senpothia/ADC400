
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import gnu.io.CommPortIdentifier; 
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent; 
import gnu.io.SerialPortEventListener; 
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

// Interfaçage R/Java

import java.util.Enumeration;
import org.rosuda.JRI.REXP;
import org.rosuda.JRI.RVector;
import org.rosuda.JRI.Rengine;


public class ADC400 extends javax.swing.JFrame implements SerialPortEventListener{

    /**
     * Creates new form ADC400
     */
    
    private static final String DEMARER="1";
    private static final String PAUSE="2";
    private static final String STOP="3";
    private static final String ACQ="A";
    private static final String AVORT="Z";
    private double ATT_V=3;   // Attenuation mesure Vout
    private double ATT_I=1;   // Attenuation mesure Iout
    private String inputLine;
    
    private int Test=1;
    
    private static  double Usup=0;
    private static  double Uinf=0;
    
     private static  double Iinf=0;
     private static  double Isup=0;
    
    //Variables de connexion
    private OutputStream output=null;
    private BufferedReader input;
    SerialPort serialPort;
    private String PORT="COM";
    
    private static final int TIMEOUT=2000; //Milisegundos
    
    private int DATA_RATE=0;
    
    private static final ImageIcon Interro=new ImageIcon("Interro.png");
    private static final ImageIcon progression=new ImageIcon("progress.png");
    private static final ImageIcon OK=new ImageIcon("good.png");
    private static final ImageIcon KO=new ImageIcon("bad.png");
   
     private static final ImageIcon Plot_I=new ImageIcon("Image_I.jpeg");
     private static final ImageIcon Plot_U=new ImageIcon("Image_U.jpeg");
     
    private int Tableau1 []={0,0,0,0,0,};   // Tableau des statistiques globales en numérique
    private String Tab1 []={"0","0","0","0","0"};  // Tableau des statistiques globales en caractères
    String Tab2 [][]={  // Tableau des résultats de séquences en caractères
        {"NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA"},
        {"NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA"},
        {"NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA"},
        {"NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA"},
        {"NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA"},
        {"NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA"},
        {"NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA"},
        {"NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA"},
        {"NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA"},
        {"NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA"},
      
                                                };

    private String[] SeqEnCours;//={"0","0","0","0","0","0","0","0","0","0","0","0","0","0"};
    
    private int CompteurSeq=0;
    
    private double Def_Vs0=0;
    private double Def_VsIs=0;
    private double Def_Is=0;
    private double Def_Rel=0;
    
    private double p_Def_Vs0=0;
    private double p_Def_VsIs=0;
    private double p_Def_Is=0;
    private double p_Def_Rel=0;
     
    String EtatNO;
    String EtatNF; 
    
    boolean flag_test=FALSE;
    //boolean flag_PAUSE=FALSE;
    
    // Fichier de sauvegarde
    
    //private String nomFichier;
    private FileWriter fluxSortie;
    private BufferedWriter Sortie;
    private File Repertoire;
    private String nomFichier;
    private String Produit, of, Sceance;
    private String Entete="Num;Vs0;Vs0_Volt;Rel2;VsIs;VsIs_Volt;Is;Is_amp;Rel5;VsIs_off;VsIs_off_Volt;Is_off;Is_off_Amp;Rel8";
    private String LigneEnCours;
    Rengine rengine=new Rengine(new String[]{"--vanilla"},false,null);
    
    public ADC400() {
        initComponents();
        
        setTitle("TEST ADC400");
        SeqEnCours=new String[14] ;
        String SeqEnCours[]={"0","0","0","0","0","0","0","0","0","0","0","0","0","0"};
       
    }

    public void initConnexion(){  // Méthode pour connexion RS232
        CommPortIdentifier portID=null;
        Enumeration portEnum=CommPortIdentifier.getPortIdentifiers();
        
        while(portEnum.hasMoreElements()){
            CommPortIdentifier actualPortID=(CommPortIdentifier) portEnum.nextElement();
            if(PORT.equals(actualPortID.getName())){
                portID=actualPortID;
                break;
            }
        }
        
        if(portID==null){
            montrerError("Connexion impossible. Vérifier les paramètres de connexion");
            
        }
        
        try{
            serialPort = (SerialPort) portID.open(this.getClass().getName(), TIMEOUT);
            //Paramètre port série
            
            serialPort.setSerialPortParams(DATA_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            
            input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
            output = serialPort.getOutputStream();
            
            //input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
        
            serialPort.addEventListener(this);
            serialPort.notifyOnDataAvailable(true);
        
        } 
        
           
        
        catch(Exception e){
            montrerError(e.getMessage());
            //System.exit(ERROR);
            
        }
    }
    
    
    public synchronized void close() {
        
		if (serialPort != null) {
			serialPort.removeEventListener();
			serialPort.close();
		}
	}
    
    
    
    public  void serialEvent(SerialPortEvent oEvent) {
		if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
                    
			try {
                         
                             inputLine=input.readLine();
                            
                             
                             if (Test<9){  // Traitement des trames reçues
                             
                             Indicateur.setText(inputLine);  // Affichage dans la ligne de texte
                             System.out.println(Test+": "+inputLine);  // Affichage à la console
                             
                             
                             
                             
                             }
                            
			} 
                        
                        catch (Exception e) {   // Traitement des exceptions
                            
				System.err.println(e.toString());
			}
                        
                            analyse(Test, inputLine);  // Analyse des résultats reçus dans la trame
                        
		}
		
	}
    
    
    
    private void envoyerData(String data){
        try{
            output.write(data.getBytes());
            
        } 
        
        catch(Exception e){
            montrerError("ERROR: Vérifier les paramètres de connexion");
            Réinitialisation();
            
        }
    }    
    
    public void initFichier(){
        
        
      // Initialisation flux de sortie
        try {    
            fluxSortie = new FileWriter(nomFichier);
            Sortie = new BufferedWriter(fluxSortie);
            
            Sortie.write("Num;Vs0;Vs0_Volt;Rel2;VsIs;VsIs_Volt;Is;Is_amp;Rel5;VsIs_off;VsIs_off_Volt;Is_off;Is_off_Amp;Rel8");
            Sortie.newLine();
            
          
        } catch (Exception ex) {
            
           
        }
      
       
                
    }
    
    private void sauvegarder(String chaine){
    
        
    try {
    //
    Sortie.write(chaine);
    Sortie.newLine();
   
    }
    
    catch (IOException ex) {
              Logger.getLogger(ADC400.class.getName()).log(Level.SEVERE, null, ex);
          }
    }
    
  private void analyse(int numTestenCours, String Trame){  // analyse de la trame reçue
      
      // Décomposition de la trame
      //envoyerData(ACQ);  // Message d'acquitement
      
      String NumTest;
      String Tension;
      String Courant;
      //String EtatNO;
      //String EtatNF;
     
      
      char NumSeq[]=new char[10];
      char Vs[]={' ',' ',' ',' ',' '};
      char Is[]={' ',' ',' ',' ',' '};
      char NO[]={' ',' ',' ',' ',' '};
      char NF[]={' ',' ',' ',' ',' '};
      
      double Vout; 
      double Iout;
      
      int i=0;
      
      if (Trame.charAt(i)=='?'){  // La trame correspond à une réponde de test
          
          i++;
          int j=0;
          while (Trame.charAt(i)!=':') { //récupération numéro de séquence de test
          
          NumSeq[j]=Trame.charAt(i);
          i++;
          j++;
          
          }
          
       i++;
       j=0;
       while (Trame.charAt(i)!=':') { //récupération tension mesurée
          Vs[j]=Trame.charAt(i);
          i++;
          j++;
          }
   
       i++;
       j=0;
       while (Trame.charAt(i)!=':') { //récupération courant mesurée
          Is[j]=Trame.charAt(i);
          i++;
          j++;
          }
      
       i++;
       j=0;
       while (Trame.charAt(i)!=':') { //récupération état contact NO
          NO[j]=Trame.charAt(i);
          i++;
          j++;
          }
       
       i++;
       j=0;
       
       while (Trame.charAt(i)!='!') { //récupération état contact NF
          NF[j]=Trame.charAt(i);
          i++;
          j++;
          }
    
      }
      
      
         NumTest = new String(NumSeq);
         System.out.println("Test en cours:"+NumTest);
          
         Tension = new String(Vs);
         Tension=Tension.trim();
         Vout = Integer.parseInt(Tension);
         System.out.println("Tension mesurée:"+(ATT_V*Vout*5/1024));
         
         Courant = new String(Is);
         Courant=Courant.trim();
         Iout = Integer.parseInt(Courant);
         System.out.println("Courant mesuré:"+(ATT_I*Iout*5/1024));
         
         EtatNO = new String(NO);
         EtatNO=EtatNO.trim();
         System.out.println("NO:"+EtatNO);
         
         EtatNF = new String(NF);
         EtatNF=EtatNF.trim();
         System.out.println("NF:"+EtatNF);
         
         
      
         
      // Traitement des éléments de la trame
      
      switch (numTestenCours){   // Traitement des phases de test
          
     
      case 1:
              
        // Test tension de sortie
          
          Tension(Vout,Res1,Res2);
          
         
        // Test courant de sortie
              // NON TRAITE
          
        // Test état NO et NF
              
          // NON TRAITE
        
       
              
              break;
      
      
       case 2:
              
        // Test tension de sortie
           
            // NON TRAITE
        
        // Test courant de sortie
              
            // NON TRAITE
       
        
        // Test état NO et NF
           
          Relais("1","1",Res2,Res3);
          
              break;
      
      
       case 3:
              
        // Test tension de sortie
          Tension(Vout,Res3,Res4);
        
        // Test courant de sortie
              
         // NON TRAITE
          
        //Test état NO et NF
            
           
          // NON TRAITE
              
              break;
              
       case 4:
              
        // Test tension de sortie
              
       // NON TRAITE
        // Test courant de sortie
             
        Courant(Iout,Res4,Res5);  
        
        // Test état NO et NF
           
          // NON TRAITE
              
              break;
              
        case 5:
              
        // Test tension de sortie
       // NON TRAITE
            
        // Test courant de sortie
        // NON TRAITE     
               
       // Test état NO et NF
            
           Relais("1","1",Res5,Res6);
              
              break;
      
              
        case 6:
              
        // Test tension de sortie
             Tension(Vout,Res6,Res7); 
        
        // Test courant de sortie
              
              // NON TRAITE
        // Test état NO et NF
            
          // NON TRAITE
            
              break;       
              
       case 7:
              
        // Test tension de sortie
       // NON TRAITE
        
        // Test courant de sortie
         Courant(Iout,Res7,Res8);       
              
      // Test état NO et NF
           
         // NON TRAITE
              break;       
              
      
        case 8:
              
        // Test tension de sortie
            
           // NON TRAITE
      
        
        // Test courant de sortie
              
     // NON TRAITE  
       // Test état NO et NF
            
            Relais("1","1",Res8,Res8);
              
              break;
              
      }
      
     
      // Acquitement
      //envoyerData(ACQ);  // Message d'acquitement
      
     
  }
  
  public void Tension(double V,javax.swing.JLabel lab1,javax.swing.JLabel lab2 ){
  
  // Test tension de sortie
  
            double Vsortie;
           
             Vsortie=ATT_V*V*5/1024;
             // Affichage des valeurs numériques en fonction de la phase de test en cours
             if( Test==1){SeqEnCours[2]=String.valueOf(Vsortie);}
             if( Test==3){SeqEnCours[5]=String.valueOf(Vsortie);}
             if( Test==6){SeqEnCours[10]=String.valueOf(Vsortie);}
          
          if (Vsortie<Usup && Vsortie>Uinf){   // Test conforme
      
              lab1.setIcon(OK);
              lab2.setIcon(progression);
              envoyerData(ACQ);  // Message d'acquitement
              // Affichage des avis de conformité en fonction de la phase de test en cours
              if (Enregistrement.isSelected()){
              if( Test==1){SeqEnCours[1]="C";}
              if( Test==3){SeqEnCours[4]="C";}
              if( Test==6){SeqEnCours[9]="C";}
              }
              Test++;   // incrémentation phase de test
            }
          
          else           // Test non conforme
              
          {  
              lab1.setIcon(KO);
              Indicateur.setText("PRODUIT NON CONFORME");
              // Affichage des avis de conformité en fonction de la phase de test en cours
              if (Enregistrement.isSelected()){
              if( Test==1){SeqEnCours[1]="NC";
                            Def_Vs0++;}
              if( Test==3){SeqEnCours[4]="NC";
                            Def_Vs0++; }
              if( Test==6){SeqEnCours[9]="NC";
                            Def_Vs0++;}
              }
              Réinitialisation();
             
            
          }        
  }
  
  public void Courant(double I,javax.swing.JLabel lab1,javax.swing.JLabel lab2 ){
  
  // Test tension de sortie
  
            double Isortie;
           
             Isortie=ATT_I*I*5/1024;
              // Affichage des valeurs numériques en fonction de la phase de test en cours
              if (Enregistrement.isSelected()){
              if( Test==4) {SeqEnCours[7]=String.valueOf(Isortie);}
              if( Test==7){SeqEnCours[12]=String.valueOf(Isortie);}
              }
          if (Isortie<Isup && Isortie>Iinf){   // Test conforme
      
              lab1.setIcon(OK);
              lab2.setIcon(progression);
               envoyerData(ACQ);  // Message d'acquitement
               
                // Affichage des avis de conformité en fonction de la phase de test en cours
              if (Enregistrement.isSelected()){  
              if( Test==4) {SeqEnCours[6]="C";}
              if( Test==7){SeqEnCours[11]="C";}
              }
               Test++;
            }
          
          else           // Test non conforme
              
          {  
              lab1.setIcon(KO);
              Indicateur.setText("PRODUIT NON CONFORME");
              
              // Affichage des avis de conformité en fonction de la phase de test en cours
            if (Enregistrement.isSelected()){  
               if( Test==4) {SeqEnCours[6]="NC";
                             Def_Is++;}
               if( Test==7){SeqEnCours[11]="NC";
                             Def_Is++;}
            }
              Réinitialisation();
               
          }        
  
  }
  
   public void Relais(String N1,String N2,javax.swing.JLabel lab1,javax.swing.JLabel lab2){
       
       if (Test<8){
       
       if (EtatNO.compareToIgnoreCase(N1)==0 && EtatNF.compareToIgnoreCase(N2)==0){  // Test conforme
             lab1.setIcon(OK);
              lab2.setIcon(progression);
              envoyerData(ACQ);  // Message d'acquitement
              
               // Affichage des avis de conformité en fonction de la phase de test en cours
               if (Enregistrement.isSelected()){
              if( Test==2) {SeqEnCours[3]="C";}
              if( Test==5) {SeqEnCours[8]="C";}
               }
              Test++;
          
            }
       
       else {        // Test non conforme
              lab1.setIcon(KO);
              Indicateur.setText("PRODUIT NON CONFORME");
            if (Enregistrement.isSelected()){  
              if( Test==2) {SeqEnCours[3]="NC";
                            Def_Rel++;}
              if( Test==5) {SeqEnCours[8]="NC";
                            Def_Rel++;}
            }
              Réinitialisation();}
       }  
       
       else { // Test = 8 - Dernière séquence
           
           if (EtatNO.compareToIgnoreCase(N1)==0 && EtatNF.compareToIgnoreCase(N2)==0 ){  // Test conforme
         lab1.setIcon(OK);
         Indicateur.setText("PRODUIT CONFORME");
         JOptionPane.showMessageDialog(this, "PRODUIT CONFORME", "CONFORME", JOptionPane.INFORMATION_MESSAGE);
         if (Enregistrement.isSelected()){SeqEnCours[13]="C";}
         Test++;
         Réinitialisation();}
          
           
           else { lab1.setIcon(KO);   // Test non conforme
              Indicateur.setText("PRODUIT NON CONFORME");
              if (Enregistrement.isSelected()){
              SeqEnCours[13]="NC";
                       Def_Rel++;
              }
              Réinitialisation();}
         
       }
   }
  
  public void Réinitialisation() {  // Séquence d'initialisation
  
    
      if (Test<9) {JOptionPane.showMessageDialog(this, "NON CONFORME", "ALERTE", JOptionPane.WARNING_MESSAGE);}  
      // Séquence interrompu par une non conformité  (Test<9)
      
   // Rétablissement boutons de commande
   
      BDemarrer.setVisible(TRUE);
      BPause.setVisible(FALSE);
      BStop.setVisible(FALSE);
      
   // Rétablissement des icônes de progression test   
      Res1.setIcon(Interro);
      Res2.setIcon(Interro);
      Res3.setIcon(Interro);
      Res4.setIcon(Interro);
      Res5.setIcon(Interro);
      Res8.setIcon(Interro);
      Res7.setIcon(Interro);
      Res6.setIcon(Interro);
      Indicateur.setText("Vous pouvez lancer une autre séquence de test");
      
      if (Test==10){envoyerData(STOP);  // Test arrêté par opérateur
      JOptionPane.showMessageDialog(this, "Séquence annulée", "ARRET", JOptionPane.INFORMATION_MESSAGE);}
      
      else { envoyerData(AVORT);  // Message d'avortement test en cours 
      }
      
      if (Enregistrement.isSelected() && Test!=10){  
          // Mode enregistrement
          // Modification de Tab2 - reconstitution des lignes
          // Le mode enregistrement et selectionné et le test n'a pas été STOPPER par l'opérateur
          int i=8;
          while (i>-1){
              
              System.arraycopy(Tab2[i], 0, Tab2[i+1], 0, 14);
              i--;
          }
          
          CompteurSeq++; // Incrémentation séquence
          SeqEnCours[0]=String.valueOf(CompteurSeq);
          System.arraycopy(SeqEnCours, 0,Tab2[0] , 0, 14);
          
          
          // Affichage du tableau 2
          
          for (int k=0; k<10;k++){        // k=indice de ligne
              
              for (int j=0; j<14;j++){        // j=indice de colonne
                  
                  TAB2.setValueAt(Tab2[k][j],k,j);
              }
          }
          
          
          
          // Comptage tableau 1
          
          p_Def_Vs0=100*(Def_Vs0/CompteurSeq);
          p_Def_Is=100*(Def_Is/CompteurSeq);
          p_Def_VsIs=100*(Def_VsIs/CompteurSeq);
          p_Def_Rel=100*(Def_Rel/CompteurSeq);
          
          TAB1.setValueAt(CompteurSeq,0,0);
          TAB1.setValueAt(String.valueOf(p_Def_Vs0)+"%",0,1);
          TAB1.setValueAt(String.valueOf(p_Def_VsIs)+"%",0,2);
          TAB1.setValueAt(String.valueOf(p_Def_Is)+"%",0,3);
          TAB1.setValueAt(String.valueOf(p_Def_Rel)+"%",0,4);
          
          // Sauvegarde dans fichier txt
          
          
          //sauvegarder("Hola!!!");
          LigneEnCours=SeqEnCours[0]+";"+SeqEnCours[1]+";"+SeqEnCours[2]+";"+SeqEnCours[3]+";"+SeqEnCours[4]+";"+SeqEnCours[5]+";"+SeqEnCours[6]+";"+SeqEnCours[7]+";"+SeqEnCours[8]+";"+SeqEnCours[9]+";"+SeqEnCours[10]+";"+SeqEnCours[11]+";"+SeqEnCours[12]+";"+SeqEnCours[13];
          sauvegarder(LigneEnCours);
        
          
          //System.out.println("LigneEnCours:"+LigneEnCours);
          
         
          
     }
      Compteur.setText(String.valueOf(CompteurSeq));  // Affichage nbre de séquences abouties
      flag_test=FALSE;  // Fin de séquence - attente nouveau démmarrage
  }
  
 
 
 
      
       
       
    public void montrerError(String message){
        JOptionPane.showMessageDialog(this, message, "ERROR", JOptionPane.ERROR_MESSAGE);
    }
    
   
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        SelectFichier = new javax.swing.JFileChooser();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        OngTest = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jPanel3 = new javax.swing.JPanel();
        BDemarrer = new javax.swing.JButton();
        BPause = new javax.swing.JButton();
        BStop = new javax.swing.JButton();
        BFermer = new javax.swing.JButton();
        Enregistrement = new javax.swing.JRadioButton();
        jPanel4 = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        Indicateur = new javax.swing.JTextField();
        Res1 = new javax.swing.JLabel();
        Res2 = new javax.swing.JLabel();
        Res3 = new javax.swing.JLabel();
        Res4 = new javax.swing.JLabel();
        Res5 = new javax.swing.JLabel();
        Res6 = new javax.swing.JLabel();
        Res8 = new javax.swing.JLabel();
        Res7 = new javax.swing.JLabel();
        Compteur = new javax.swing.JTextField();
        jLabel20 = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        NumPort = new javax.swing.JComboBox<>();
        jLabel9 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        Baud = new javax.swing.JComboBox<>();
        BConnxion = new javax.swing.JButton();
        jLabel12 = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        Prod = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        OF = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        Operateur = new javax.swing.JTextField();
        BValidationEng = new javax.swing.JButton();
        jLabel17 = new javax.swing.JLabel();
        Serie = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jPanel10 = new javax.swing.JPanel();
        ATU = new javax.swing.JTextField();
        ATI = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        BValideMesures = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        Um = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        UM = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        Im = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        IM = new javax.swing.JTextField();
        jPanel8 = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        jPanel12 = new javax.swing.JPanel();
        jLabel18 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        TAB1 = new javax.swing.JTable();
        jScrollPane3 = new javax.swing.JScrollPane();
        TAB2 = new javax.swing.JTable();
        jLabel19 = new javax.swing.JLabel();
        jPanel14 = new javax.swing.JPanel();
        jLabel23 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        Table_Et_U = new javax.swing.JTable();
        Graphe_U = new javax.swing.JLabel();
        BCalcul_U = new javax.swing.JButton();
        jLabel25 = new javax.swing.JLabel();
        InterU = new javax.swing.JTextField();
        jLabel26 = new javax.swing.JLabel();
        dydxU = new javax.swing.JTextField();
        jPanel13 = new javax.swing.JPanel();
        jLabel21 = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        Table_Et_I = new javax.swing.JTable();
        BCalcul_I = new javax.swing.JButton();
        Graphe_I = new javax.swing.JLabel();
        InterI = new javax.swing.JTextField();
        dydxI = new javax.swing.JTextField();
        jLabel22 = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();

        SelectFichier.setDialogTitle("Dossier de sauvegarde");
        SelectFichier.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
        SelectFichier.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SelectFichierActionPerformed(evt);
            }
        });

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(255, 255, 255));

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        jLabel1.setBackground(new java.awt.Color(255, 255, 255));
        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(0, 0, 255));
        jLabel1.setText("TEST ALIMENTATION ADC400");

        OngTest.setBackground(new java.awt.Color(255, 255, 255));
        OngTest.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        OngTest.setPreferredSize(new java.awt.Dimension(100, 600));

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel1.setPreferredSize(new java.awt.Dimension(900, 900));

        jScrollPane1.setBorder(null);
        jScrollPane1.setEnabled(false);
        jScrollPane1.setOpaque(false);
        jScrollPane1.setWheelScrollingEnabled(false);

        jTextArea1.setColumns(20);
        jTextArea1.setFont(new java.awt.Font("Monospaced", 1, 18)); // NOI18N
        jTextArea1.setRows(5);
        jTextArea1.setText("1 - Mesures à vide\n\t\n- Tension de sortie\n\n- Etat relais\n\n2- Mesure en charge\n\n- Tension de sortie\n\n- Courant de sortie\n\n- Etat relais\n\n3- Défaut secteur\n\n- Tension sortie\n\n- Courant de sortie \n\n- Etat relais\n\t");
        jTextArea1.setBorder(null);
        jScrollPane1.setViewportView(jTextArea1);

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));

        BDemarrer.setBackground(new java.awt.Color(255, 255, 255));
        BDemarrer.setIcon(new javax.swing.ImageIcon(getClass().getResource("/play.png"))); // NOI18N
        BDemarrer.setBorderPainted(false);
        BDemarrer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BDemarrerActionPerformed(evt);
            }
        });

        BPause.setBackground(new java.awt.Color(255, 255, 255));
        BPause.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pause.png"))); // NOI18N
        BPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BPauseActionPerformed(evt);
            }
        });

        BStop.setBackground(new java.awt.Color(255, 255, 255));
        BStop.setIcon(new javax.swing.ImageIcon(getClass().getResource("/stop.png"))); // NOI18N
        BStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BStopActionPerformed(evt);
            }
        });

        BFermer.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        BFermer.setText("Fermer");
        BFermer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BFermerActionPerformed(evt);
            }
        });

        Enregistrement.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        Enregistrement.setText("Enregistrer");
        Enregistrement.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EnregistrementActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(BStop, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(BPause, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(BDemarrer, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(BFermer, javax.swing.GroupLayout.DEFAULT_SIZE, 89, Short.MAX_VALUE)
                .addGap(38, 38, 38))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(Enregistrement, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(30, 30, 30))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(Enregistrement)
                .addGap(42, 42, 42)
                .addComponent(BDemarrer, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(BPause)
                .addGap(18, 18, 18)
                .addComponent(BStop)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 39, Short.MAX_VALUE)
                .addComponent(BFermer)
                .addGap(32, 32, 32))
        );

        jPanel4.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 72, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jPanel9.setBackground(new java.awt.Color(37, 165, 225));

        Indicateur.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        Indicateur.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        Indicateur.setText("Lancez une séquence");
        Indicateur.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                IndicateurActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel9Layout.createSequentialGroup()
                .addContainerGap(122, Short.MAX_VALUE)
                .addComponent(Indicateur, javax.swing.GroupLayout.PREFERRED_SIZE, 354, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(94, 94, 94))
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(Indicateur, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        Res1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Interro.png"))); // NOI18N

        Res2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Interro.png"))); // NOI18N

        Res3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Interro.png"))); // NOI18N

        Res4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Interro.png"))); // NOI18N

        Res5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Interro.png"))); // NOI18N

        Res6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Interro.png"))); // NOI18N

        Res8.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Interro.png"))); // NOI18N

        Res7.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Interro.png"))); // NOI18N

        Compteur.setText("0");
        Compteur.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CompteurActionPerformed(evt);
            }
        });

        jLabel20.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel20.setText("Compteur:");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                        .addComponent(Res1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(504, 504, 504))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(539, 539, 539))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(Res2)
                            .addComponent(Res3)
                            .addComponent(Res4)
                            .addComponent(Res5)
                            .addComponent(Res6)
                            .addComponent(Res8)
                            .addComponent(Res7))
                        .addGap(119, 119, 119)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 276, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(37, 37, 37)
                        .addComponent(jLabel20)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(Compteur, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(534, 534, 534))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(168, 168, 168)
                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(37, 37, 37))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(93, 93, 93)
                        .addComponent(Res1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Res2)
                        .addGap(63, 63, 63)
                        .addComponent(Res3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Res4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Res5)
                        .addGap(67, 67, 67)
                        .addComponent(Res6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(Res7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(Res8))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(47, 47, 47)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 543, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(68, 68, 68)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(Compteur, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel20))
                        .addGap(18, 18, 18)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        OngTest.addTab("TEST", jPanel1);

        jPanel6.setBackground(new java.awt.Color(255, 255, 255));

        jLabel10.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(0, 0, 255));
        jLabel10.setText("Paramètres de communication");

        jPanel5.setBackground(new java.awt.Color(255, 255, 255));

        NumPort.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "COM3", "COM1", "COM2", "COM4" }));
        NumPort.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        jLabel9.setText("Port:");

        jLabel11.setText("Baud:");

        Baud.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "9600", "19200", "115200" }));

        BConnxion.setText("Valider");
        BConnxion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BConnxionActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(79, 79, 79)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel11)
                    .addComponent(jLabel9))
                .addGap(18, 18, 18)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(BConnxion, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(Baud, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(NumPort, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(128, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(NumPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(Baud, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(BConnxion)
                .addContainerGap(18, Short.MAX_VALUE))
        );

        jLabel12.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(0, 51, 255));
        jLabel12.setText("Enregistrements");

        jPanel7.setBackground(new java.awt.Color(255, 255, 255));

        Prod.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        Prod.setText("ADC400");
        Prod.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ProdActionPerformed(evt);
            }
        });

        jLabel13.setText("Produit:");

        OF.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        OF.setText("24");

        jLabel14.setText("OF:");

        jLabel15.setText("Opérateur:");

        Operateur.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        Operateur.setText("???");

        BValidationEng.setText("Valider");
        BValidationEng.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BValidationEngActionPerformed(evt);
            }
        });

        jLabel17.setText("Scéance:");

        Serie.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        Serie.setText("5");
        Serie.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SerieActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGap(139, 139, 139)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                        .addComponent(jLabel13)
                        .addGap(18, 18, 18)
                        .addComponent(Prod, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                        .addComponent(jLabel14)
                        .addGap(18, 18, 18)
                        .addComponent(OF, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(BValidationEng, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel17)
                            .addComponent(jLabel15))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(Operateur, javax.swing.GroupLayout.DEFAULT_SIZE, 87, Short.MAX_VALUE)
                            .addComponent(Serie))))
                .addContainerGap(182, Short.MAX_VALUE))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Prod, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel13))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(OF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel14))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Operateur, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel15))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel17)
                    .addComponent(Serie, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 20, Short.MAX_VALUE)
                .addComponent(BValidationEng)
                .addContainerGap())
        );

        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(0, 0, 255));
        jLabel3.setText("Paramètres de mesures");

        jPanel10.setBackground(new java.awt.Color(255, 255, 255));

        ATU.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        ATU.setText("3.0");
        ATU.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ATUActionPerformed(evt);
            }
        });

        ATI.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        ATI.setText("1.0");

        jLabel4.setText("Attenuateur tension:");

        jLabel5.setText("Attenuateur courant:");

        BValideMesures.setText("Valider");
        BValideMesures.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BValideMesuresActionPerformed(evt);
            }
        });

        jLabel6.setText("Umin:");

        Um.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        Um.setText("11.5");

        jLabel7.setText("Umax:");

        UM.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        UM.setText("12.5");

        jLabel8.setText("Imin:");

        Im.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        Im.setText("2.5");

        jLabel16.setText("Imax:");

        IM.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        IM.setText("3.5");

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(ATU, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addGap(18, 18, 18)
                        .addComponent(ATI, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(53, 53, 53)
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(Um)
                    .addComponent(Im, javax.swing.GroupLayout.DEFAULT_SIZE, 47, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel7)
                    .addComponent(jLabel16))
                .addGap(18, 18, 18)
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(UM)
                    .addComponent(IM, javax.swing.GroupLayout.DEFAULT_SIZE, 45, Short.MAX_VALUE))
                .addContainerGap(164, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel10Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(BValideMesures, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(263, 263, 263))
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ATU, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(jLabel6)
                    .addComponent(Um, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7)
                    .addComponent(UM, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(ATI, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8)
                    .addComponent(Im, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel16)
                    .addComponent(IM, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(BValideMesures)
                .addContainerGap(47, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addGap(202, 202, 202))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 177, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(242, 242, 242))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                        .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(111, 111, 111))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                        .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(180, 180, 180))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 227, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(229, 229, 229))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                        .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(30, 30, 30))))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(42, 42, 42)
                .addComponent(jLabel10)
                .addGap(18, 18, 18)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(40, 40, 40)
                .addComponent(jLabel12)
                .addGap(18, 18, 18)
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel3)
                .addGap(29, 29, 29)
                .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        OngTest.addTab("CONFIGURATION", jPanel6);

        jPanel8.setBackground(new java.awt.Color(255, 255, 255));
        jPanel8.setForeground(new java.awt.Color(0, 0, 255));

        jPanel11.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 651, Short.MAX_VALUE)
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 86, Short.MAX_VALUE)
        );

        jPanel12.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 651, Short.MAX_VALUE)
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 428, Short.MAX_VALUE)
        );

        jLabel18.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel18.setForeground(new java.awt.Color(0, 0, 255));
        jLabel18.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel18.setText("STATISTIQUES GENERALES");

        TAB1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        TAB1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null}
            },
            new String [] {
                "Nbre", "Vs @ 0A", "Vs @ Is", "Is ", "Relais"
            }
        ));
        TAB1.setGridColor(new java.awt.Color(0, 0, 0));
        TAB1.setSelectionForeground(new java.awt.Color(0, 0, 0));
        jScrollPane2.setViewportView(TAB1);
        TAB1.setShowVerticalLines(TRUE);
        TAB1.setShowHorizontalLines(TRUE);

        TAB2.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED, null, new java.awt.Color(0, 0, 0), null, null));
        TAB2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null, null, null}
            },
            new String [] {
                "N° ", "Vs @ 0A", "Volt", "Rel-2", "Vs @ Is", "Volt", "Is", "Amp", "Rel-5", "Vs - ~ off", "Volt", "Is - ~ off", "Amp", "Rel-8"
            }
        ));
        TAB2.setGridColor(new java.awt.Color(0, 0, 0));
        TAB2.setSelectionBackground(new java.awt.Color(255, 0, 0));
        jScrollPane3.setViewportView(TAB2);
        TAB2.setShowVerticalLines(TRUE);
        TAB2.setShowHorizontalLines(TRUE);

        jLabel19.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel19.setForeground(new java.awt.Color(0, 51, 255));
        jLabel19.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel19.setText("RESULTATS EN COURS");

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel8Layout.createSequentialGroup()
                        .addGap(78, 78, 78)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 554, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel8Layout.createSequentialGroup()
                        .addGap(265, 265, 265)
                        .addComponent(jLabel18))
                    .addGroup(jPanel8Layout.createSequentialGroup()
                        .addGap(24, 24, 24)
                        .addComponent(jPanel11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel8Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane3))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel8Layout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel12, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(34, 34, 34))
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGap(269, 269, 269)
                .addComponent(jLabel19)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGap(33, 33, 33)
                .addComponent(jLabel18)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel19)
                .addGap(40, 40, 40)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 198, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel12, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        OngTest.addTab("STATISTIQUES", jPanel8);

        jPanel14.setBackground(new java.awt.Color(255, 255, 255));

        jLabel23.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel23.setForeground(new java.awt.Color(0, 0, 255));
        jLabel23.setText("Etalonnage mesure de tension");

        Table_Et_U.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Table_Et_U.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Ref", null, null, null, null, null, null, null, null, null, null},
                {"Mes", null, null, null, null, null, null, null, null, null, null}
            },
            new String [] {
                "", "Pt 1", "Pt 2", "Pt 3", "Pt 4", "Pt 5", "Pt 6", "Pt 7", "Pt 8", "Pt 9", "Pt 10"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        Table_Et_U.setGridColor(new java.awt.Color(0, 0, 0));
        jScrollPane4.setViewportView(Table_Et_U);
        Table_Et_U.setShowVerticalLines(TRUE);
        Table_Et_U.setShowHorizontalLines(TRUE);

        BCalcul_U.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        BCalcul_U.setText("Calculer");
        BCalcul_U.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BCalcul_UActionPerformed(evt);
            }
        });

        jLabel25.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel25.setText("Intercept:");

        jLabel26.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel26.setText("dy/dx:");

        javax.swing.GroupLayout jPanel14Layout = new javax.swing.GroupLayout(jPanel14);
        jPanel14.setLayout(jPanel14Layout);
        jPanel14Layout.setHorizontalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel14Layout.createSequentialGroup()
                .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel14Layout.createSequentialGroup()
                        .addGap(250, 250, 250)
                        .addComponent(jLabel23, javax.swing.GroupLayout.PREFERRED_SIZE, 257, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel14Layout.createSequentialGroup()
                        .addGap(41, 41, 41)
                        .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(Graphe_U, javax.swing.GroupLayout.PREFERRED_SIZE, 680, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 666, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(35, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel14Layout.createSequentialGroup()
                .addGap(85, 85, 85)
                .addComponent(jLabel25, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(InterU, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel26)
                .addGap(18, 18, 18)
                .addComponent(dydxU, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(59, 59, 59)
                .addComponent(BCalcul_U)
                .addGap(87, 87, 87))
        );
        jPanel14Layout.setVerticalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel14Layout.createSequentialGroup()
                .addGap(38, 38, 38)
                .addComponent(jLabel23)
                .addGap(31, 31, 31)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(26, 26, 26)
                .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(BCalcul_U)
                    .addComponent(jLabel25)
                    .addComponent(InterU, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel26)
                    .addComponent(dydxU, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(Graphe_U, javax.swing.GroupLayout.DEFAULT_SIZE, 479, Short.MAX_VALUE)
                .addContainerGap())
        );

        OngTest.addTab("Métrologie tension", jPanel14);

        jPanel13.setBackground(new java.awt.Color(255, 255, 255));

        jLabel21.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel21.setForeground(new java.awt.Color(0, 0, 255));
        jLabel21.setText("Etalonnage mesure de courant");

        Table_Et_I.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        Table_Et_I.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Ref", null, null, null, null, null, null, null, null, null, null},
                {"Mes", null, null, null, null, null, null, null, null, null, null}
            },
            new String [] {
                "", "Pt 1", "Pt 2", "Pt 3", "Pt 4", "Pt 5", "Pt 6", "Pt 7", "Pt 8", "Pt 9", "Pt 10"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        Table_Et_I.setGridColor(new java.awt.Color(0, 0, 0));
        jScrollPane5.setViewportView(Table_Et_I);
        Table_Et_I.setShowVerticalLines(TRUE);
        Table_Et_I.setShowHorizontalLines(TRUE);

        BCalcul_I.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        BCalcul_I.setText("Calculer");
        BCalcul_I.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BCalcul_IActionPerformed(evt);
            }
        });

        jLabel22.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel22.setText("Intercept:");

        jLabel24.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel24.setText("dy/dx:");

        javax.swing.GroupLayout jPanel13Layout = new javax.swing.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addGap(253, 253, 253)
                .addComponent(jLabel21)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel13Layout.createSequentialGroup()
                .addContainerGap(41, Short.MAX_VALUE)
                .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 669, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Graphe_I, javax.swing.GroupLayout.PREFERRED_SIZE, 681, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel13Layout.createSequentialGroup()
                        .addGap(37, 37, 37)
                        .addComponent(jLabel22, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(InterI, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(29, 29, 29)
                        .addComponent(jLabel24)
                        .addGap(18, 18, 18)
                        .addComponent(dydxI, javax.swing.GroupLayout.PREFERRED_SIZE, 157, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(28, 28, 28)
                        .addComponent(BCalcul_I)))
                .addGap(34, 34, 34))
        );
        jPanel13Layout.setVerticalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addGap(45, 45, 45)
                .addComponent(jLabel21)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(29, 29, 29)
                .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(BCalcul_I)
                    .addComponent(InterI, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(dydxI, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel22)
                    .addComponent(jLabel24))
                .addGap(26, 26, 26)
                .addComponent(Graphe_I, javax.swing.GroupLayout.DEFAULT_SIZE, 475, Short.MAX_VALUE)
                .addContainerGap())
        );

        OngTest.addTab("Métrologie courant", jPanel13);

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Michel.png"))); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(38, Short.MAX_VALUE)
                .addComponent(jLabel2)
                .addGap(97, 97, 97)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 319, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(164, 164, 164))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(OngTest, javax.swing.GroupLayout.PREFERRED_SIZE, 761, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel2))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(26, 26, 26)
                        .addComponent(jLabel1)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(OngTest, javax.swing.GroupLayout.PREFERRED_SIZE, 738, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(60, 60, 60))
        );

        OngTest.getAccessibleContext().setAccessibleName("TEST");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void SelectFichierActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SelectFichierActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_SelectFichierActionPerformed

    private void BValideMesuresActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BValideMesuresActionPerformed
        // TODO add your handling code here:

        // Configuration des paramètres de mesures

        String Umini,Umaxi;
        String Imini,Imaxi;
        String ATT_V_St, ATT_I_St;

        Umini=Um.getText();
        Uinf=Double.parseDouble(Umini);
        System.out.println("Uinf:"+Uinf);
        Umaxi=UM.getText();
        Usup=Double.parseDouble(Umaxi);
        System.out.println("sup:"+Usup);
        Imini=Im.getText();
        Iinf=Double.parseDouble(Imini);
        System.out.println("Iinf:"+Iinf);
        Imaxi=IM.getText();
        Isup=Double.parseDouble(Imaxi);
        System.out.println("Isup:"+Isup);

        ATT_V_St=ATU.getText();
        ATT_V=Double.parseDouble(ATT_V_St);
        System.out.println("ATT_V:"+ATT_V);

        ATT_I_St=ATI.getText();
        ATT_I=Double.parseDouble(ATT_I_St);
        System.out.println("ATT_I:"+ATT_I);

    }//GEN-LAST:event_BValideMesuresActionPerformed

    private void ATUActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ATUActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_ATUActionPerformed

    private void SerieActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SerieActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_SerieActionPerformed

    private void BValidationEngActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BValidationEngActionPerformed

        int showOpenDialog = SelectFichier.showOpenDialog(this);

        Repertoire= SelectFichier.getSelectedFile();
        Produit=Prod.getText();
        of=OF.getText();
        Sceance=Serie.getText();
        nomFichier=Repertoire + "\\" +Produit+"_"+of+"_"+Sceance+".txt";
        System.out.println(Repertoire);
        System.out.println(nomFichier);
        initFichier();

    }//GEN-LAST:event_BValidationEngActionPerformed

    private void ProdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ProdActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_ProdActionPerformed

    private void BConnxionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BConnxionActionPerformed
        // TODO add your handling code here:
        // Aqcuisition des paramètres de connexion
        String DATA_RATE_string;
        PORT=NumPort.getSelectedItem().toString();
        DATA_RATE_string=Baud.getSelectedItem().toString();
        DATA_RATE= Integer.parseInt(DATA_RATE_string);
        System.out.println("PORT SELECTIONNE:"+PORT);
        System.out.println("BAUD RATE:"+DATA_RATE);
        initConnexion();

    }//GEN-LAST:event_BConnxionActionPerformed

    private void CompteurActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CompteurActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_CompteurActionPerformed

    private void IndicateurActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_IndicateurActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_IndicateurActionPerformed

    private void EnregistrementActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EnregistrementActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_EnregistrementActionPerformed

    private void BFermerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BFermerActionPerformed
        // TODO add your handling code here:

        JOptionPane.showMessageDialog(this, "Voulez-vous fermer ce programme?", "Fermeture programme", JOptionPane.INFORMATION_MESSAGE);
        try {
            Sortie.close();
        } catch (IOException ex) {
            Logger.getLogger(ADC400.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.exit(0);
    }//GEN-LAST:event_BFermerActionPerformed

    private void BStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BStopActionPerformed
        // TODO add your handling code here:
        flag_test=FALSE; // Il n'y a plus de test en cours
        BStop.setVisible(FALSE);
        BDemarrer.setVisible(TRUE);
        BPause.setVisible(FALSE);
        Indicateur.setText("Séquence de test arrêté.");
        Test=10;

        Réinitialisation();
    }//GEN-LAST:event_BStopActionPerformed

    private void BPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BPauseActionPerformed
        // TODO add your handling code here:
        BDemarrer.setVisible(TRUE);
        BPause.setVisible(FALSE);
        BStop.setVisible(TRUE);
        Indicateur.setText("Test interromptu. En attente.");

        envoyerData(PAUSE);
        JOptionPane.showMessageDialog(this, "Test interrompu", "PAUSE", JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_BPauseActionPerformed

    private void BDemarrerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BDemarrerActionPerformed
        // TODO add your handling code here:

        if (flag_test==FALSE){   // Retour après un STOP ou fin d'une séquence
            Test=1;

            for (int i=0; i<14;i++){ // Réinitialisation des résultats en cours

                SeqEnCours[i]="NA";

            }

            //if (Enregistrement.isSelected()){ CompteurSeq++; } // Incrémentation du nbre de séquences}

        }

        if (PORT=="COM" && DATA_RATE==0){  // La comm n'est pas configurée

            JOptionPane.showMessageDialog(this, "Initialisez les paramètres de connexion", "INIT COM", JOptionPane.INFORMATION_MESSAGE);
        }

        else {   // Les mesures ne sont pas configurées

            if(Uinf==0 || Usup==0 || Iinf==0 || Isup==0 ){

                JOptionPane.showMessageDialog(this, "Initialisez les paramètres de mesures", "MESURES", JOptionPane.INFORMATION_MESSAGE);

            }

            else {  // Tout est configuré

                BDemarrer.setVisible(FALSE);
                BPause.setVisible(TRUE);
                BStop.setVisible(TRUE);

                Indicateur.setText("Test en cours... Patientez");
                flag_test=TRUE; // Un test est en cours
                envoyerData(DEMARER);

                Sequence();

            }
        }

    }//GEN-LAST:event_BDemarrerActionPerformed

    private void BCalcul_UActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BCalcul_UActionPerformed
        // TODO add your handling code here:
        
    //Rengine rengine=new Rengine(new String[]{"--vanilla"},false,null);        // TODO add your handling code here:
    
    double [] x =new double[10];
    double [] y=new double[10];
    
    int i=1;
    Object I;
    while (i<11){
     //for (int i=1; i<10; i++){
     I=Table_Et_U.getValueAt(0,i);
     x[i-1]=Double.parseDouble((String) I);
     i++;
    }
    
    for (int j=0; j<10; j++){
    System.out.println(x[j]);
    }
    
    i=1;
    Object J;
    while (i<11){
     //for (int i=1; i<10; i++){
     J=Table_Et_U.getValueAt(1,i);
     y[i-1]=Double.parseDouble((String) J);
     i++;
    }
    
    for (int j=0; j<10; j++){
    System.out.println(y[j]);
    }
    
    rengine.assign("x", x);
    rengine.assign("y", y);
    rengine.eval("mod_U<-lm(y ~ x)");
     rengine.eval("predict(mod_U, interval=\"confidence\")");
    rengine.eval("jpeg(\"Image_U.jpeg\", width = 670, height = 470, units = \"px\")");
    rengine.eval("plot(x,y,xlab=\"Références\",ylab=\"Mesures\",main=\"Etalonage tension\")");
    rengine.eval("abline(mod_U)");
    rengine.eval("segments(x,fitted(mod_U),x,y)");
   
    rengine.eval("pred.frame<-data.frame(x)");
    rengine.eval("pc<-predict(mod_U, interval=\"confidence\",newdata=pred.frame)");
    rengine.eval("print(pc)");
    rengine.eval("pp<-predict(mod_U, interval=\"prediction\",newdata=pred.frame)");
    rengine.eval("print(pp)");
    rengine.eval("matlines(pred.frame, pc[,2:3], lty=c(2,2), col=\"blue\")");
    rengine.eval("matlines(pred.frame, pp[,2:3], lty=c(3,3), col=\"red\")");
    rengine.eval("dev.off()");
    rengine.eval("res<-coef(mod_U)");
    double [] coef = rengine.eval("res").asDoubleArray();
    
    System.out.println(coef[0]);
    System.out.println(coef[1]);
    System.out.println("ok");
    
    InterU.setText(String.valueOf(coef[0]));
    dydxU.setText(String.valueOf(coef[1]));
    
   Date temps_init=new Date() ;
   long duree;
    duree=temps_init.getTime();
   
    while(duree<1000){
     duree=temps_init.getTime();
    }
    while(!rengine.waitForR()){
     ;
    }
    Graphe_U.setIcon(Plot_U);
   
       
    }//GEN-LAST:event_BCalcul_UActionPerformed

    private void BCalcul_IActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BCalcul_IActionPerformed
    
    // TODO add your handling code here:
        
    //Rengine rengine=new Rengine(new String[]{"--vanilla"},false,null);        // TODO add your handling code here:
    
    double [] x =new double[10];
    double [] y=new double[10];
    
    int i=1;
    Object I;
    while (i<11){
     //for (int i=1; i<10; i++){
     I=Table_Et_I.getValueAt(0,i);
     x[i-1]=Double.parseDouble((String) I);
     i++;
    }
    
    for (int j=0; j<10; j++){
    System.out.println(x[j]);
    }
    
    i=1;
    Object J;
    while (i<11){
     //for (int i=1; i<10; i++){
     J=Table_Et_I.getValueAt(1,i);
     y[i-1]=Double.parseDouble((String) J);
     i++;
    }
    
    for (int j=0; j<10; j++){
    System.out.println(y[j]);
    }
    
    rengine.assign("x", x);
    rengine.assign("y", y);
    rengine.eval("mod_I<-lm(y ~ x)");
    rengine.eval("predict(mod_I, interval=\"confidence\")");
    rengine.eval("jpeg(\"Image_I.jpeg\", width = 670, height = 470, units = \"px\")");
    rengine.eval("plot(x,y,xlab=\"Références\",ylab=\"Mesures\",main=\"Etalonage courant\")");
    rengine.eval("abline(mod_I)");
    rengine.eval("segments(x,fitted(mod_I),x,y)");
   
    rengine.eval("pred.frame<-data.frame(x)");
    rengine.eval("pc<-predict(mod_I, interval=\"confidence\",newdata=pred.frame)");
    rengine.eval("print(pc)");
    rengine.eval("pp<-predict(mod_I, interval=\"prediction\",newdata=pred.frame)");
    rengine.eval("print(pp)");
    rengine.eval("matlines(pred.frame, pc[,2:3], lty=c(2,2), col=\"blue\")");
    rengine.eval("matlines(pred.frame, pp[,2:3], lty=c(3,3), col=\"red\")");
    rengine.eval("dev.off()");
    rengine.eval("res<-coef(mod_I)");
    double [] coef = rengine.eval("res").asDoubleArray();
  
    System.out.println(coef[0]);
    System.out.println(coef[1]);
    System.out.println("ok");
    
    InterI.setText(String.valueOf(coef[0]));
    dydxI.setText(String.valueOf(coef[1]));
    System.out.println("ok");
    
   Date temps_init=new Date() ;
   long duree;
    duree=temps_init.getTime();
   
    while(duree<5000){
     duree=temps_init.getTime();
    }
    
     while(!rengine.waitForR()){
     ;
    }
    
    Graphe_I.setIcon(Plot_I);
   
       
    }//GEN-LAST:event_BCalcul_IActionPerformed

    
    private void Sequence() {
    
    // Méthode de déroulement d'une séquence de test
     
    switch (Test){
    
        case 1:
            
             Res1.setIcon(progression);
             break;
             
        case 2:
            
             Res2.setIcon(progression);
             break;
        
        case 3:
            
             Res3.setIcon(progression);
             break;
             
        case 4:
            
            Res4.setIcon(progression);
             break;
    
         case 5:
            
            Res5.setIcon(progression);
             break;    
             
         case 6:
            
            Res8.setIcon(progression);
             break;
             
         case 7:
            
            Res7.setIcon(progression);
             break;
             
        case 8:
            
            Res6.setIcon(progression);
             break;     
             
    }
    
    
    }
    
   
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ADC400.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            new ADC400().setVisible(true);
        });
        
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField ATI;
    private javax.swing.JTextField ATU;
    private javax.swing.JButton BCalcul_I;
    private javax.swing.JButton BCalcul_U;
    private javax.swing.JButton BConnxion;
    private javax.swing.JButton BDemarrer;
    private javax.swing.JButton BFermer;
    private javax.swing.JButton BPause;
    private javax.swing.JButton BStop;
    private javax.swing.JButton BValidationEng;
    private javax.swing.JButton BValideMesures;
    private javax.swing.JComboBox<String> Baud;
    private javax.swing.JTextField Compteur;
    private javax.swing.JRadioButton Enregistrement;
    private javax.swing.JLabel Graphe_I;
    private javax.swing.JLabel Graphe_U;
    private javax.swing.JTextField IM;
    private javax.swing.JTextField Im;
    private javax.swing.JTextField Indicateur;
    private javax.swing.JTextField InterI;
    private javax.swing.JTextField InterU;
    private javax.swing.JComboBox<String> NumPort;
    private javax.swing.JTextField OF;
    private javax.swing.JTabbedPane OngTest;
    private javax.swing.JTextField Operateur;
    private javax.swing.JTextField Prod;
    private javax.swing.JLabel Res1;
    private javax.swing.JLabel Res2;
    private javax.swing.JLabel Res3;
    private javax.swing.JLabel Res4;
    private javax.swing.JLabel Res5;
    private javax.swing.JLabel Res6;
    private javax.swing.JLabel Res7;
    private javax.swing.JLabel Res8;
    private javax.swing.JFileChooser SelectFichier;
    private javax.swing.JTextField Serie;
    private javax.swing.JTable TAB1;
    private javax.swing.JTable TAB2;
    private javax.swing.JTable Table_Et_I;
    private javax.swing.JTable Table_Et_U;
    private javax.swing.JTextField UM;
    private javax.swing.JTextField Um;
    private javax.swing.JTextField dydxI;
    private javax.swing.JTextField dydxU;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JTextArea jTextArea1;
    // End of variables declaration//GEN-END:variables

    
}
