
// *****************   Variables globales ********************************

int n=0;                               // Etape de test en cours
int ledPin = 13;                       // Sortie LED indicatrice
int ledState = LOW;                    // Etat de la LED (LOW par défaut)
int NO;
int NF;

char mot[5];   // Le mot lu sur la liaison série
char mot1[5]; 

int Vs=0;        // Tension de sortie
int Is=0;        // Courant de sortie

//                   Entrées analogiques
int Vout= 0;          // Broche AN0
int Courant=1;        // Broche AN1


//                    Entrée-Sorties numériques    

int CommutCharge=2;  // Relais de commutation de la charge
int BatCharge=7;     // Relais de mise en charge batterie
int CommutBat=8;     // Commutation batterie 
int EtatNO=0;        // Etat relais (NO)
int EtatNF=1;        // Etat relais (NF)

//                      Trame d'émission

String TRAME232;

 // ***************   SETUP : Configuration liaison série *************************

void setup() {  
  // put your setup code here, to run once:
  
 Serial.begin(9600);                   // Liaison série
 pinMode(ledPin, OUTPUT);              // Pin 13 en sortie
 pinMode(CommutCharge, OUTPUT);        // Commande commutation de la charge
 pinMode(BatCharge, OUTPUT);           // Commande de mise en charge de la batterie
 pinMode(CommutBat, OUTPUT);           // Commande de commutation de la batterie sur la sortie
 
 pinMode(EtatNO, INPUT);               // Lecture de l'état du relais - position NO
 pinMode(EtatNF, INPUT);               // Lecture de l'état du relais - position NF

 // Conditions initiales 

// Tous les relais sont désactivés

 digitalWrite(CommutCharge, LOW);
 digitalWrite(BatCharge, LOW);
 digitalWrite(CommutBat, LOW);
 
}

// ****************     LOOP: Boucle principale ***********************************

void loop() {  
  // put your main code here, to run repeatedly:
  
  TestReception();  // Quel ordre reçu? 1,2 ou 3?
  
      if (strcmp(mot,"1") == 0 ) {Test();}     
}

// ****************   Interrogation liaison série ***************************

void TestReception() {

     int i = 0; //variable locale pour l'incrémentation des données du tableau
            //on lit les caractères tant qu'il y en a
          //OU si jamais le nombre de caractères lus atteint 4 (limite du tableau stockant le mot - 1 caractère)

    if (Serial.available() > 0){
          
    while (Serial.available() > 0 && i <= 4)
      {
          mot[i] = Serial.read(); //on enregistre le caractère lu
          delay(10); //laisse un peu de temps entre chaque accès a la mémoire
          i++; //on passe à l'indice suivant
       }
        mot[i] = '\0'; //on supprime le caractère '\n' et on le remplace par celui de fin de chaine '\0'
    }
  // Alternatives en fonction de l'ordre
  
      if (strcmp(mot,"1") == 0 ){
            if (n==0) {n++;
                      } // Intialisation comptage séquence de test         
    }
  
   // Ordre PAUSE
        if (strcmp(mot,"2") == 0 ){
      }

    // Ordre STOP
        if (strcmp(mot,"3") == 0 ){
         n=0;
         ledState = LOW;  
         digitalWrite(ledPin, ledState);   
         
         // Retour aux conditions initiales 

          // Tous les relais sont désactivés

            digitalWrite(CommutCharge, LOW);
            digitalWrite(BatCharge, LOW);
            digitalWrite(CommutBat, LOW);
            }

    }

  // ********************   Execution de la séquence de test *********************

void Test(){

 switch (n){
  
// Séquence test 1 - Mesure tension de sortie à vide
case 1: 
  ledState = HIGH;  
  digitalWrite(ledPin, ledState);
  delay(1000);
  // Aucune commutation de relais
  
  Mesures();
  
  //n=2;
  ledState = LOW;  
  digitalWrite(ledPin, ledState);
  delay(1000);
   
  break;
 

  // Séquence test 2 - Test état du relais
 case 2:
  ledState = HIGH;  
  digitalWrite(ledPin, ledState);
  delay(1000);
  
   Mesures();
  
  //n=3;
  ledState = LOW;  
  digitalWrite(ledPin, ledState);
  delay(1000);

  break;

   // Séquence test 3 - Mesure tension de sortie en charge
 case 3:
 
   ledState = HIGH;  
   digitalWrite(ledPin, ledState);
   delay(1000);
  // Commutation relais charge et relais batterie

  digitalWrite(CommutCharge, HIGH);
  digitalWrite(BatCharge, LOW);
  digitalWrite(CommutBat, HIGH);
  
   Mesures();
  
   // n=4;
    ledState = LOW;  
   digitalWrite(ledPin, ledState);
   delay(1000);

   break;

   // Séquence test 4 - Mesure courant de sortie
  case 4:
 
    ledState = HIGH;  
   digitalWrite(ledPin, ledState);
   delay(1000);

   Mesures();
  
   //n=5;
   ledState = LOW;  
   digitalWrite(ledPin, ledState);
   delay(1000);

   break;

  // Séquence test 5 - Test état du relais
  case 5:
  
  ledState = HIGH;  
  digitalWrite(ledPin, ledState);
  delay(1000);

   Mesures();
  
  
 // n=6;
  ledState = LOW;  
   digitalWrite(ledPin, ledState);
  delay(1000);

   break;

// Séquence test 6 - Mesure tension de sortie - Defaut secteur
 case 6:
 
  ledState = HIGH;  
   digitalWrite(ledPin, ledState);
  delay(1000);

   Mesures();
  
 
  //n=7;
  ledState = LOW;  
  digitalWrite(ledPin, ledState);
  delay(1000);
   //}

   break;

// Séquence test 7 - Mesure courant de sortie - Défaut secteur
  case 7:

  ledState = HIGH;  
  digitalWrite(ledPin, ledState);
  delay(1000);

  Mesures();
  
 
  //n=8;
  ledState = LOW; 
   digitalWrite(ledPin, ledState); 
  delay(1000);
   //}

   break;

// Séquence test 8 - Test état relais - Défaut secteur
   case 8:

  ledState = HIGH;  
   digitalWrite(ledPin, ledState);
  delay(1000);
  
  Mesures();
  
 
  n=0;    // Toutes les séquences de test se sont déroulées - prêt pour nouvel exemplaire à tester
  mot[5]="aaaaa";
  ledState = LOW;  
  digitalWrite(ledPin, ledState);
  delay(1000);

  // Remise des relais à l'état initial

  digitalWrite(CommutCharge, LOW);
  digitalWrite(BatCharge, LOW);
  digitalWrite(CommutBat, LOW);
  
 
  break;

 }

}

// Exécution des mesures
   
 void Mesures(){

  Vs=0;
  Is=0;
  
 for (int i=0;i<10;i++){
 
  // Mesure tension de sortie - Moyenne sur 10 échantillons
  Vs=Vs+(analogRead(Vout)/10);
  
 }

for (int i=0;i<10;i++){
 
  // Mesure courant de sortie - Moyenne sur 10 échantillons
  Is=Is+(analogRead(Is)/10);
  
 }

 
  NO=digitalRead(EtatNO);     // Lecture état contact NO
  NF=digitalRead(EtatNF);     // Lecture état contact NO

  // Constitution de la trame 

  TRAME232=String(String("?")+String(n)+String(":")+String(Vs)+String(":")+String(Is)+String(":")+String(NO)+String(":")+String(NF)+String("!"));
  
   Serial.println(TRAME232);

   // Attendre ordre d'acquitement 
      int i=0;
    while (Serial.available() == 0){  // Boucle d'attente - Attente acquitement
    }

       
           while (Serial.available() > 0 && i <= 4)
      {
          mot1[i] = Serial.read(); //on enregistre le caractère lu
          delay(10); //laisse un peu de temps entre chaque accès a la mémoire
          i++; //on passe à l'indice suivant
       }
        mot1[i] = '\0'; //on supprime le caractère '\n' et on le remplace par celui de fin de chaine '\0'
    
    if (strcmp(mot1,"A") == 0 ){ // Message acquitement reçu

          n++;
         mot1[5]="aaaaa";
    }
            
      if (strcmp(mot1,"Z") == 0 ){ // Message AVORT reçu

          n=0;
         mot1[5]="aaaaa";
         mot[5]="aaaaa";

         // Retour aux conditions initiales 

         // Tous les relais sont désactivés

        digitalWrite(CommutCharge, LOW);
        digitalWrite(BatCharge, LOW);
        digitalWrite(CommutBat, LOW);
    }       
   
 }



 

