#!/usr/bin/perl -w

use strict;
use warnings;

use Data::Dumper;
$Data::Dumper::Indent = 1;
$Data::Dumper::Sortkeys = 1;

use constant TRUE  => 1;
use constant FALSE => 0;

my %data = ();
my $model;

my $logfile = "/home/pdsanchez/Proyectos/cmodel/log/cmodel_0.log";
open (IN, $logfile) or die "No se abre $logfile";
while (my $line = <IN>) {
	chomp $line;
	if ($line =~ /^.+\[INFO - ModelParsing\] parseSBML: \+\+\+ Reading model \d+: (.+\.xml)\s+$/) {
		$model = $1;
	}
	elsif ($line =~ /^.+\[INFO - ModelParsing\] normalizeSBML: \>\>\> Comp: (.+) - (.+)\s+$/) {
		$data{$model}{TCOMP}++;
	}
	elsif ($line =~ /^.+\[INFO - CompoundNormalization\] normalize: MAPPING (FOUND|NOTFOUND|DUDE|DB FOUND).+$/) {
		$data{$model}{TOTAL}{$1}++;
	}
}
close (IN);


foreach my $md (sort keys %data) {
	my $t = $data{$md}{TCOMP};

	print "MODEL\t$md\n";
	print "TOTAL COMPOUNDS\t$t\n";
	foreach my $st (sort keys %{$data{$md}{TOTAL}}) {
		my $tst = $data{$md}{TOTAL}{$st};
		my $pc = $tst*100/$t;
		printf("TOTAL %s\t%d\t%.0f%%\n", $st, $tst, $pc);
	}
	print "EOR\n";
}

exit 0;
