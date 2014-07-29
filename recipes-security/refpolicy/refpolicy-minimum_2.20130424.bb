PR = "r99"

include refpolicy-targeted_${PV}.bb

SUMMARY = "SELinux minimum policy"
DESCRIPTION = "\
This is a minimum reference policy with just core policy modules, and \
could be used as a base for customizing targeted policy. \
Pretty much everything runs as initrc_t or unconfined_t so all of the \
domains are unconfined. \
"

POLICY_NAME = "minimum"

FILESEXTRAPATHS_prepend := "${THISDIR}/files:${THISDIR}/refpolicy-${PV}:${THISDIR}/refpolicy-targeted:"

CORE_POLICY_MODULES = "unconfined \
	selinuxutil storage sysnetwork \
	application libraries miscfiles logging userdomain \
	init mount modutils getty authlogin locallogin \
	"

# nscd caches libc-issued requests to the name service.
# Without nscd.pp, commands want to use these caches will be blocked.
EXTRA_POLICY_MODULES += "nscd"

# pam_mail module enables checking and display of mailbox status upon
# "login", so "login" process will access to /var/spool/mail.
EXTRA_POLICY_MODULES += "mta"

POLICY_MODULES_MIN = "${CORE_POLICY_MODULES} ${EXTRA_POLICY_MODULES}"

# re-write the same func from refpolicy_common.inc
prepare_policy_store () {
	oe_runmake 'DESTDIR=${D}' 'prefix=${D}${prefix}' install

	# Prepare to create policy store
	mkdir -p ${D}${sysconfdir}/selinux/
	mkdir -p ${D}${sysconfdir}/selinux/${POLICY_NAME}/policy
	mkdir -p ${D}${sysconfdir}/selinux/${POLICY_NAME}/modules/active/modules
	mkdir -p ${D}${sysconfdir}/selinux/${POLICY_NAME}/contexts/files
	touch ${D}${sysconfdir}/selinux/${POLICY_NAME}/contexts/files/file_contexts.local
	if  ${@base_contains('DISTRO_FEATURES','compressed_policy','true','false',d)}; then
		bzip2 base.pp
		cp base.pp.bz2 ${D}${sysconfdir}/selinux/${POLICY_NAME}/modules/active/base.pp
		for i in ${POLICY_MODULES_MIN}; do
			bzip2 $i
			cp ${i}.bz2 ${D}${sysconfdir}/selinux/${POLICY_NAME}/modules/active/modules/`basename $i`
		done
	else
		bzip2 -c ${D}${datadir}/selinux/${POLICY_NAME}/base.pp  > \
			${D}${sysconfdir}/selinux/${POLICY_NAME}/modules/active/base.pp
		for i in ${POLICY_MODULES_MIN}; do
			bzip2 -c ${D}${datadir}/selinux/${POLICY_NAME}/$i.pp > \
				${D}${sysconfdir}/selinux/${POLICY_NAME}/modules/active/modules/$i.pp
		done
	fi
}
