#!/bin/sh
# AUTO-GENERATED FILE, DO NOT EDIT!
if [ -f $1.org ]; then
  sed -e 's!^F:/cygwin/lib!/usr/lib!ig;s! F:/cygwin/lib! /usr/lib!ig;s!^F:/cygwin/bin!/usr/bin!ig;s! F:/cygwin/bin! /usr/bin!ig;s!^F:/cygwin/!/!ig;s! F:/cygwin/! /!ig;s!^V:!/cygdrive/v!ig;s! V:! /cygdrive/v!ig;s!^F:!/cygdrive/f!ig;s! F:! /cygdrive/f!ig;s!^C:!/cygdrive/c!ig;s! C:! /cygdrive/c!ig;' $1.org > $1 && rm -f $1.org
fi
