GHOSTLAB : Jeu similaire au célèbre jeu PAC-MAN en clients/serveur : les clients étant des joueurs visant à avoir le maximum de points et le serveur gérant les parties du jeu réalisé en C et en JAVA (Serveur en JAVA et client en C) 

# Pour Compiler le projet :

- Se placer à la racine du Projet
- Executer la commande make pour compiler tout les programmes C et JAVA

# Pour Executer le projet :

- Se placer au répertoire racine du projet et ouvrir deux terminaux
- lancer le serveur dans un terminal avec : java serveur.Serveur
- lancer le client dans un deuxième terminal avec : ./client

# Utilisation : 

Pour utiliser notre Client/Serveur, il suffit de suivre les exemplaires de requetes
données dans le sujet du projet, par exemple pour s'enregistrer à une partie de jeu 1
il faut entrer en ligne de commande la requette : REGIS username 4512 1 (exemple)
des messages d'erreurs s'affiche sur le terminal en cas d'erreur de saisie ou si le
joueur tente de faire une requete qui ne lui est pas auorisée.

## Architecture du projet: ##

Chaque client qui se connecte à notre serveur, on lui associe un thread qui assure la communication joueur/serveur
Si un client fait une requete NEWPL ou REGIS, on crée un objet de type joueur (classe Joueur.java) avec un id et un port unique et
on l'inscrit à une seule partie ( classe Partie.java)
La classe CommunicateFunctions contient des fonctions qui assurent la connexion à travers des socket et la communication
avec le serveur ( reception/envoi)
Traitement.java: assure la création de nouvelles parties ou l'inscription à des parties déjà existante ainsi que l'envoie
des listes des parties
Format.java: assure les transformation en octets avant l'envoie
Labyrinthe.java: initialise un labyrinthe, le remplir aléatoirement, gère les mouvement des joueurs et des fantomes dans le labyrinthe,
suppression des fantomes
