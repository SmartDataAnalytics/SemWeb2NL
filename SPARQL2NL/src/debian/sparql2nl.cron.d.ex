#
# Regular cron jobs for the sparql2nl package
#
0 4	* * *	root	[ -x /usr/bin/sparql2nl_maintenance ] && /usr/bin/sparql2nl_maintenance
