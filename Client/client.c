#include <sys/socket.h>
#include <netinet/in.h>
#include <stdlib.h>
#include <unistd.h>
#include <stdio.h>
#include <string.h>
#include <netdb.h>
#include <pthread.h>
#include <arpa/inet.h>
#include <ncurses.h>
#include <signal.h>

#define BUFF_MAX_SIZE 200
#define MESS_MAX_SIZE 400

pthread_t multicast_thread;
pthread_t udp_thread;
pthread_mutex_t verrou = PTHREAD_MUTEX_INITIALIZER;

int localhost = 1;
int sock;           // socket de connection
char **wordTab;     // on l'utilise pour stocker les messages reçu dans un tableau de mots
char buff_in[1024]; // buffer utilisé pour la lecture sur le terminal
int boolean = 1;

// A utiliser pour Ncurses
short x = 0; // input
short y = 0; // print
short max_y = 0;

void closeConnection(int exitCode, char *error)
{
    if (error != NULL)
    {
        perror(error);
    }
    endwin();
    close(sock);
    exit(exitCode);
}

uint8_t strToUint8(const char *s)
{
    if (strcmp(s, "0") == 0)
        return 0;
    else
        return s[0];
}

uint16_t strToUint16(const char *s)
{
    if (strcmp(s, "0") == 0)
        return 0;
    else
        return (unsigned char)(s[0] << 8) | s[1];
}

char *strConvert(char *s, ssize_t len)
{
    char *str = malloc(40);
    if (len == 1)
    {
        sprintf(str, " %d", strToUint8(s));
    }
    else if (len == 2)
    {
        sprintf(str, " %d", strToUint16(s));
    }
    else
    {
        sprintf(str, " %s", s);
    }
    return str;
}

int stringToWordTab(char *str, char ***res)
{
    char str2[strlen(str)];
    strcpy(str2, str);
    int len = 0;
    size_t max_len = 0;

    char *msg = strtok(str2, " ");
    while (msg != NULL)
    {
        len++;
        if (max_len < strlen(msg))
        {
            max_len = strlen(msg);
        }
        msg = strtok(NULL, " ");
    }

    char **tab = malloc(len * max_len);
    int i = 0;
    strcpy(str2, str);
    msg = strtok(str2, " ");
    while (msg != NULL)
    {
        tab[i] = malloc(strlen(msg));
        tab[i] = strcpy(tab[i], msg);
        i++;
        msg = strtok(NULL, " ");
    }
    free(*res); // on libère d'abord le tableau
    *res = tab;
    return len;
}

// on lit a partir du terminal en utilisant des fonctions de ncurses, et on mets ce qu'on a lu dans stdin_buff
ssize_t readTerminal(char *strLu)
{
    while (1)
    {
        int c = getch(); // on lit un caractères
        if (!boolean)
            return -1;
        if (c == ERR)
            return -1;
        if (max_y == y)
            move(y - 1, 0); // move : pour deplacer le curseur
        else
            move(y, 0);
        clrtoeol();

        if (c == 10)
        { // bouton : ENTREE
            strcpy(strLu, buff_in);
            int len = x;
            x = 0;
            strcpy(buff_in, "");
            printw("%s", buff_in);
            refresh();
            return len;
        }
        else if (c == 127)
        { // shift : supprimer une lettre
            buff_in[x] = '\0';
            if (x > 0)
                x -= 1;
        }
        else
        {
            const char *k = keyname(c);
            if (strlen(k) == 1 && k[0] != '^' && k[0] != '[')
            {
                strcat(buff_in, keyname(c));
                x += 1;
            }
        }
        printw("%s", buff_in);
        refresh();
    }
}

// fonction d'affichage
void print(char *fmt, ...)
{
    va_list args;
    va_start(args, fmt);
    char str[1024];
    vsprintf(str, fmt, args);
    va_end(args);
    pthread_mutex_lock(&verrou);
    if (y == max_y)
    {
        move(y - 1, 0);
        clrtoeol();
        printw("%s\n", str);
    }
    else
    {
        move(y, 0);
        clrtoeol();
        printw("%s\n", str);
        y += 1;
    }
    move(y, 0);
    clrtoeol();
    printw("%s\n", buff_in);
    move(y, x);
    refresh();
    pthread_mutex_unlock(&verrou);
}

// fonction pour recevoir la réponse du serveur
char *recevoir()
{
    char *buff = malloc(BUFF_MAX_SIZE);
    buff = strcpy(buff, "");
    int len = 0;
    int cpt = 0; // compter les étoiles à la fin du msg
    char mot[40];
    strcpy(mot, "");
    int len_mot = 0;
    ssize_t size = recv(sock, buff, 5, 0); // on reçoit d'abord la requete
    buff[size] = '\0';
    len += (int)size;

    while (boolean)
    { // on lit ensuite caractères par caractère
        char c[2];
        size = recv(sock, c, 1, 0);
        if (size <= 0)
        {
            if (boolean)
                closeConnection(EXIT_FAILURE, "recv");
            else
                closeConnection(EXIT_SUCCESS, NULL);
        }
        if (size == 1 && c[0] == '\0')
        {
            c[0] = '0';
        }
        c[1] = '\0';
        if (c[0] == '*' || c[0] == '+')
        {
            if (cpt == 2)
            {
                if (len_mot > 0)
                {
                    buff = strcat(buff, strConvert(mot, len_mot));
                }
                len++;
                break;
            }
            else
                cpt++;
        }
        else if (c[0] == ' ')
        {
            if (len_mot > 0)
            {
                buff = strcat(buff, strConvert(mot, len_mot));
                strcpy(mot, "");
                len_mot = 0;
            }
        }
        else
        {
            strcat(mot, c);
            len_mot++;
        }
        len++;
    }
    return buff;
}
void listeParties(char *str)
{
    print("%s", strcat(str, "***"));
    stringToWordTab(str, &wordTab);
    int n = atoi(wordTab[1]);
    for (int i = 0; i < n; ++i)
    {
        str = recevoir();
        print("%s", strcat(str, "***"));
    }
}

void *threadMultiDiffusion(void *arg)
{
    char(*args)[20] = arg;
    int sock = socket(PF_INET, SOCK_DGRAM, 0);
    int ok = 1;
    int r;
    if (localhost)
        r = setsockopt(sock, SOL_SOCKET, SO_REUSEPORT, &ok, sizeof(ok));
    else
        r = setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, &ok, sizeof(ok));
    if (r != 0)
    {
        print("Impossible de recevoir les messages générals");
        pthread_exit((void *)EXIT_FAILURE);
    }
    struct sockaddr_in address_sock;
    address_sock.sin_family = AF_INET;
    address_sock.sin_port = htons(atoi(args[1]));
    address_sock.sin_addr.s_addr = htonl(INADDR_ANY);
    r = bind(sock, (struct sockaddr *)&address_sock, sizeof(struct sockaddr_in));
    if (r != 0)
    {
        print("Impossible de recevoir les messages générals");
        pthread_exit((void *)EXIT_FAILURE);
    }
    struct ip_mreq mreq;
    mreq.imr_multiaddr.s_addr = inet_addr(args[0]);
    mreq.imr_interface.s_addr = htonl(INADDR_ANY);
    r = setsockopt(sock, IPPROTO_IP, IP_ADD_MEMBERSHIP, &mreq, sizeof(mreq));
    if (r != 0)
    {
        print("Impossible de recevoir les messages générals");
        pthread_exit((void *)EXIT_FAILURE);
    }
    char tampon[250];
    char **tab;
    while (boolean)
    {
        ssize_t s = recv(sock, tampon, 100, 0);
        if (s < 3)
            continue;
        if (s < 0)
            break;
        tampon[s - 3] = '\0';
        stringToWordTab(tampon, &tab);
        if (strcmp(tab[0], "GHOST") == 0)
        {
            print("GHOST %s %s", tab[1], tab[2]);
        }
        else if (strcmp(tab[0], "ENDGA") == 0)
        {
            print("ENDGA %s %s", tab[1], tab[2]);
            boolean = 0;
            break;
        }
        else if (strcmp(tab[0], "MESSA") == 0)
        {
            char msg[200];
            strncpy(msg, tampon + 15, s - 18);
            print("[GLOBAL] %s: %s", tab[1], msg);
        }
        else if (strcmp(tab[0], "SCORE") == 0)
        {
            print("SCORE %s %s %s %s",
                  tab[1], tab[3], tab[4], tab[2]);
        }
    }
    close(sock);
    pthread_exit(EXIT_SUCCESS);
}

void *threadUDP(void *arg)
{ // pour recevoir les msg en UDP
    in_port_t portUDP = *((int *)arg);
    int udp_sock = socket(PF_INET, SOCK_DGRAM, 0);
    struct sockaddr_in address_sock;
    address_sock.sin_family = AF_INET;
    address_sock.sin_port = htons(portUDP);
    address_sock.sin_addr.s_addr = htonl(INADDR_ANY);
    int r = bind(udp_sock, (struct sockaddr *)&address_sock, sizeof(struct sockaddr_in));
    if (r != 0)
    {
        print("impossible de recevoir le messages privés entre joueurs ");
        pthread_exit((void *)EXIT_FAILURE);
    }
    char tampon[300];
    char **tabUDP;
    while (boolean)
    {
        ssize_t s = recv(udp_sock, tampon, 299, 0);
        if (s < 3)
            continue;
        if (s < 0)
            break;
        tampon[s - 3] = '\0';
        stringToWordTab(tampon, &tabUDP);
        if (strcmp(tabUDP[0], "MESSP") == 0)
        {
            char msg[200];
            strncpy(msg, tampon + 15, s - 18);
            print(" MESSP %s: %s", tabUDP[1], msg);
        }
    }
    pthread_exit(EXIT_SUCCESS);
}

int main()
{
    char *port = "4243";
    char address[100] = "localhost";

    struct addrinfo *info;
    struct addrinfo hints;
    memset(&hints, 0, sizeof(hints));
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_STREAM;
    int r = getaddrinfo(address, port, &hints, &info);
    if (r != 0)
    {
        perror("addrinfo != 0");
        exit(EXIT_FAILURE);
    }
    if (info == NULL)
    {
        perror("info == NULL");
        exit(EXIT_FAILURE);
    }
    sock = socket(PF_INET, SOCK_STREAM, 0);
    if (sock == -1)
    {
        perror("ERROR: socket");
        exit(EXIT_FAILURE);
    }

    r = connect(sock, (struct sockaddr *)info->ai_addr, sizeof(struct sockaddr_in));
    if (r != 0)
    {
        close(sock);
        perror("ERROR: connect socket");
        exit(EXIT_FAILURE);
    }

    // L'affichage avec Ncurses
    struct _win_st *my_win = initscr();
    noecho();  // echo et noecho contrôlent si les caractères tapés par l'utilisateur sont renvoyés par
    refresh(); // getch au fur et à mesure qu'ils sont tapés.
    max_y = my_win->_maxy;

    listeParties(recevoir());

    char buff[BUFF_MAX_SIZE];
    char msg[MESS_MAX_SIZE];
    char rep[MESS_MAX_SIZE];
    int inscrit = 0; // si le joueur est inscrit à une partie
    int started = 0; // si le joueur est dans une partie deja commencé

    while (boolean)
    {
        strcpy(buff, "");
        strcpy(msg, "");
        strcpy(rep, "");

        print("Saisissez une requete au serveur :");

        if (readTerminal(buff) == -1)
            break;

        stringToWordTab(buff, &wordTab);
        if (strcmp(wordTab[0], "NEWPL") == 0)
        {
            if (inscrit)
            {
                print(" ATTENION : Deja inscrit à une partie ");
                continue;
            }
            in_port_t port2 = (in_port_t)atoi(wordTab[2]);
            strcat(buff, "***");
            send(sock, buff, strlen(buff), 0);
            print("%s", buff);

            strcpy(rep, recevoir());
            stringToWordTab(rep, &wordTab);
            print("%s", rep);
            if (strcmp(wordTab[0], "REGOK") == 0)
            {
                pthread_create(&udp_thread, NULL, threadUDP, (void *)&port2);
                inscrit = 1;
            }
        }
        else if (strcmp(wordTab[0], "REGIS") == 0)
        {
            if (inscrit)
            {
                print("ATTENION : Deja inscrit à une partie");
                continue;
            }
            print("%s", buff);
            in_port_t port2 = (in_port_t)atoi(wordTab[2]);
            uint8_t m = atoi(wordTab[3]);
            sprintf(msg, "REGIS %s %d ", wordTab[1], port2);
            send(sock, msg, strlen(msg), 0);
            send(sock, &m, sizeof(uint8_t), 0);
            send(sock, "***", 3, 0);

            strcpy(rep, recevoir());
            stringToWordTab(rep, &wordTab);
            print("%s", rep);
            if (strcmp(wordTab[0], "REGOK") == 0)
            {
                pthread_create(&udp_thread, NULL, threadUDP, (void *)&port2);
                inscrit = 1;
            }
        }
        else if (strcmp(wordTab[0], "UNREG") == 0)
        {
            if (!inscrit)
            {
                print("ERROR: vous n'etes inscrit a aucune partie, pour vous déconnectez du serveur saisissez DECO!");
                continue;
            }
            if (started)
            {
                print("ERROR: la partie de jeu a deja commencé, pour quitez la partie saisissez IQUIT");
                continue;
            }
            strcpy(msg, strcat(buff, "***"));
            send(sock, msg, strlen(msg), 0);
            print("%s", buff);

            strcpy(rep, recevoir());
            stringToWordTab(rep, &wordTab);
            print("%s", rep);
            if (strcmp(wordTab[0], "UNROK") == 0)
            {
                started = 0;
                inscrit = 0;
            }
        }
        else if (strcmp(wordTab[0], "SIZE?") == 0)
        {
            print("%s", buff);
            uint8_t m = atoi(wordTab[1]);
            send(sock, wordTab[0], strlen(wordTab[0]), 0);
            send(sock, &m, sizeof(uint8_t), 0);
            send(sock, "***", 3, 0);

            strcpy(rep, recevoir());
            stringToWordTab(rep, &wordTab);
            print("%s", rep);
        }
        else if (strcmp(wordTab[0], "GAME?") == 0)
        {
            strcpy(msg, strcat(buff, "***"));
            print(msg);
            send(sock, msg, strlen(msg), 0);
            strcpy(rep, recevoir());
            if (strcmp(rep, "DUNNO***") != 0)
            {
                listeParties(rep);
            }
        }
        else if (strcmp(wordTab[0], "LIST?") == 0)
        { // [LIST? m***]
            print(buff);
            uint8_t m = atoi(wordTab[1]);
            sprintf(msg, "%s ", wordTab[0]);
            send(sock, msg, 6, 0);
            send(sock, &m, sizeof(uint8_t), 0);
            send(sock, "***", 3, 0);

            strcpy(rep, recevoir());
            stringToWordTab(rep, &wordTab);
            print("%s", rep);
            if (strcmp(wordTab[0], "LIST!") == 0)
            {
                m = atoi(wordTab[1]);
                uint8_t s = atoi(wordTab[2]);
                for (int i = 0; i < s; ++i)
                {
                    strcpy(rep, recevoir());
                    print("%s", rep);
                }
            }
        }
        else if (strcmp(wordTab[0], "START") == 0)
        {
            if (!inscrit)
            {
                print("ATTENION : vous etes deja inscrit à une partie");
                continue;
            }
            if (started)
            {
                print("ERREUR: la partie a deja commencer");
                continue;
            }

            send(sock, strcat(buff, "***"), strlen(buff) + 3, 0);
            print("%s", buff);
            print("Attendez que tous les joueurs de la partie envoient START");
            strcpy(rep, recevoir());
            stringToWordTab(rep, &wordTab);
            print("%s", rep);

            if (strcmp(wordTab[0], "WELCO") == 0)
            {

                char ip[strlen(wordTab[5])];
                strcpy(ip, wordTab[5]);
                for (int i = strlen(wordTab[5]) - 1; i >= 0; i--)
                {
                    if (ip[i] == '#')
                    {
                        ip[i] = '\0';
                    }
                    else
                        break;
                }
                started = 1;
                char args[2][20];
                strcpy(args[0], ip);
                strcpy(args[1], wordTab[6]);
                pthread_create(&multicast_thread, NULL, threadMultiDiffusion, (void *)&args);

                stringToWordTab(recevoir(), &wordTab);
                if (strcmp(wordTab[0], "POSIT") == 0)
                {
                    print("%s %s %s", wordTab[0], wordTab[2], wordTab[3]);
                }
            }
        }
        else if (strcmp(wordTab[0], "UPMOV") == 0 || strcmp(wordTab[0], "LEMOV") == 0 || strcmp(wordTab[0], "DOMOV") == 0 ||
                 strcmp(wordTab[0], "RIMOV") == 0)
        {
            if (!inscrit)
            {
                print("ERREUR: vous ne jouez a aucune partie");
                continue;
            }
            if (!started)
            {
                print("ERREUR: la partie n'a pas encore commence");
                continue;
            }
            strcat(msg, buff);
            strcat(msg, "***");
            send(sock, msg, strlen(msg), 0);
            print(recevoir());
        }
        else if (strcmp(wordTab[0], "MALL?") == 0)
        {
            if (!inscrit)
            {
                print("ERREUR: vous n'etes inscrit à aucune partie");
                continue;
            }
            if (!started)
            {
                print("ATTENTION: vous ne pouvez pas envoyer des messages MALL si la partie n'est pas commencee");
                continue;
            }
            strcpy(msg, buff);
            strcat(msg, "***");
            send(sock, msg, strlen(msg), 0);

            strcpy(rep, recevoir());
            stringToWordTab(rep, &wordTab);

            if (strcmp(wordTab[0], "MALL!") == 0)
            {
                print("%s", rep);
            }
            else
            {
                print("ERREUR: votre message n'a pas été envoyé");
            }
        }
        else if (strcmp(wordTab[0], "SEND?") == 0)
        {
            if (!inscrit)
            {
                print("ERREUR: vous n'etes inscrit à aucune partie");
                continue;
            }
            strcpy(msg, buff);
            strcat(msg, "***");
            send(sock, msg, strlen(msg), 0);
            print(recevoir());
        }
        else if (strcmp(wordTab[0], "GLIS?") == 0)
        {
            if (!inscrit)
            {
                print("[ERREUR: vous n'etes inscrit à aucune partie");
                continue;
            }
            strcpy(msg, "GLIS?***");
            print("%s", msg);
            send(sock, msg, strlen(msg), 0);

            strcpy(rep, recevoir());
            stringToWordTab(rep, &wordTab);
            print(rep);
            if (strcmp(wordTab[0], "DUNNO") != 0)
            {
                uint8_t s = atoi(wordTab[1]);
                for (int i = 0; i < (int)s; ++i)
                {
                    print("%s", recevoir());
                }
            }
        }
        else if (strcmp(wordTab[0], "IQUIT") == 0)
        {
            if (!inscrit)
            {
                print("[ERROR: vous n'etes inscrit a aucune partie, pour vous déconnectez du serveur saisissez DECO!");
            }
            if (!started)
            {
                print("ERREUR: la partie de jeu n'a pas commencé, saisissez UNREG pour vous désinscrire");
                continue;
            }
            strcpy(msg, strcat(buff, "***"));
            print("%s", msg);
            send(sock, msg, strlen(msg), 0);
            print("%s", recevoir());
            closeConnection(EXIT_SUCCESS, NULL);
        }
        else if (strcmp(buff, "DECO!") == 0)
        {
            if (inscrit)
            {
                print("ERREUR: vous etes déjà inscrit à une partie, saisissez UNREG pour vous désinscrire");
                continue;
            }
            if (started)
            {
                print("ERREUR: la partie de jeu a deja commencé, saisissez IQUIT pour la quitter");
                continue;
            }
            strcpy(msg, "DECO!***");
            send(sock, msg, strlen(msg), 0);
            print("%s", recevoir());
            closeConnection(EXIT_SUCCESS, NULL);
            closeConnection(EXIT_SUCCESS, NULL);
        }
    }
    closeConnection(EXIT_SUCCESS, NULL);
}