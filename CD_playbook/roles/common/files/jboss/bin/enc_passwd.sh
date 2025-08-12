#!/bin/sh

###############################
# DataSource Password Encrypt #
###############################
# Ver #   JBOSS EAP 6.4.X     #
###############################

. ./env.sh

SNAME=$1
ACCOUNT=$2
PASSWORD=$3
DBNAME=$4

## PICKETBOX PATH SETTING ##
#PICKETBOX_PATH="modules/system/layers/base/org/picketbox/main"    ## 6.4.0
#PICKETBOX_PATH="modules/system/layers/base/.overlays/layer-base-jboss-eap-6.4.11.CP/org/picketbox/main"   ## 6.4.11
PICKETBOX_PATH="modules/system/layers/base/.overlays/layer-base-jboss-eap-6.4.16.CP/org/picketbox/main"   ## 6.4.11

## DBNAME CHECK ##
str_chk=`echo "$DBNAME" | egrep "(.*)\/(.*)$" | wc -l`

if [[ $str_chk ]];
then
        DBNAME=`echo "$DBNAME" | sed -e 's/\//\\\\\//g'`
fi



## CHECK INPUT DATA ## 
if [ e$DBNAME == "e" ];
then 
	echo " input DATASOURCE Info ....." 
        echo " ex ) ./enc_passwd_1.1.sh \"Security Domain name\" \"user name\" \"passwd\" \"datasource name\" " 
        exit 1
fi

#### DATASOURCE  PASSWORD ENCRYPT  ####
####  EAP 6.4
export CLASSPATH=$JBOSS_HOME/$PICKETBOX_PATH/picketbox-4.1.6.Final-redhat-1.jar:$JBOSS_HOME/modules/system/layers/base/org/jboss/logging/main/jboss-logging-3.1.4.GA-redhat-2.jar:$CLASSPATH

### Passwd Encryption  #####
RESULT_LOW=`java  org.picketbox.datasource.security.SecureIdentityLoginModule $PASSWORD > enc.cli`
RESULT_1=`cut -f2 -d: enc.cli`
RESULT_2=`echo $RESULT_1`
echo "/subsystem=security/security-domain=$SNAME/:add(cache-type=default)" > enc.cli
echo "/subsystem=security/security-domain=$SNAME/authentication=classic :add(login-modules=[{\"code\"=>\"org.picketbox.datasource.security.SecureIdentityLoginModule\", \"flag\"=>\"required\", \"module-options\"=>[(\"username\"=>\"$ACCOUNT\"),(\"password\"=>\"$RESULT_2\")]}])" >> enc.cli
echo "/subsystem=datasources/data-source=$DBNAME:write-attribute(name=security-domain,value=$SNAME)">> enc.cli
echo "/subsystem=datasources/data-source=$DBNAME:write-attribute(name=user-name,value=undefined)" >> enc.cli
echo "/subsystem=datasources/data-source=$DBNAME:write-attribute(name=password,value=undefined)" >> enc.cli

./jboss-cli.sh --file=enc.cli

rm -f enc.cli

##############################

echo "Security Domain name : $SNAME"
echo "DB Account : $ACCOUNT"
echo "password : $PASSWORD"
echo "Datasource pool name : $DBNAME"
echo "## END ##"

