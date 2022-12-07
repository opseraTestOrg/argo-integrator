curl -s "https://raw.githubusercontent.com/kubernetes-sigs/kustomize/master/hack/install_kustomize.sh"  | bash
mv kustomize /usr/local/bin
git clone ${giturl} --depth 1 --branch ${gitbranch}
cd ${git_file_path}
kustomize edit set image ${IMAGE_REFERENCE}=${IMAGE_URL}
git status
git config --global user.email "opsera_user"
git config --global user.name "opsera_user"
git add "kustomization.yaml"
git commit -m "updating the kustomization.yaml"
git push origin ${gitbranch}