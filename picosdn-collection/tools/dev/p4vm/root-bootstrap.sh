#!/bin/bash
set -xe

VM_TYPE=${1:-dev}

BAZEL_VER="0.15.2"
BAZEL_DEB="bazel_${BAZEL_VER}-linux-x86_64.deb"
# Create user sdn
useradd -m -d /home/sdn -s /bin/bash sdn
echo "sdn:rocks" | chpasswd
echo "sdn ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/99_sdn
chmod 440 /etc/sudoers.d/99_sdn
usermod -aG vboxsf sdn
update-locale LC_ALL="en_US.UTF-8"

if [ ${VM_TYPE} = "tutorial" ]
then
    su sdn <<'EOF'
cd /home/sdn
bash /vagrant/tutorial-bootstrap.sh
EOF
fi

# Java 8
apt-get install software-properties-common -y
add-apt-repository ppa:webupd8team/java -y
apt-get update

DEBIAN_FRONTEND=noninteractive apt-get -y -o Dpkg::Options::="--force-confdef" -o Dpkg::Options::="--force-confold" upgrade

wget https://github.com/bazelbuild/bazel/releases/download/${BAZEL_VER}/${BAZEL_DEB}
echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 select true" | debconf-set-selections
apt-get -y --no-install-recommends install \
    ./${BAZEL_DEB} \
    avahi-daemon \
    bridge-utils \
    git \
    git-review \
    htop \
    oracle-java8-installer \
    oracle-java8-set-default \
    python2.7 \
    python2.7-dev \
    valgrind \
    zip unzip \
    tcpdump \
    vlan \
    ntp \
    vim nano emacs \
    arping \
    gawk \
    texinfo \
    build-essential \
    iptables \
    automake \
    autoconf \
    libtool \
    isc-dhcp-server

DEBIAN_FRONTEND=noninteractive apt-get -yq install wireshark

rm -f ${BAZEL_DEB}

curl https://bootstrap.pypa.io/get-pip.py -o get-pip.py
python2.7 get-pip.py --force-reinstall
rm -f get-pip.py

pip install ipaddress

tee -a /etc/ssh/sshd_config <<EOF

UseDNS no
EOF

su sdn <<'EOF'
cd /home/sdn
bash /vagrant/user-bootstrap.sh
EOF
