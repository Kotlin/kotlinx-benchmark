package org.jetbrains.gradle.benchmarks

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.*

fun processPackage(
    module: ModuleDescriptor,
    packageView: PackageViewDescriptor,
    process: (ClassDescriptor) -> Unit
) {
    for (packageFragment in packageView.fragments.filter { it.module == module }) {
        DescriptorUtils.getAllDescriptors(packageFragment.getMemberScope())
            .filterIsInstance<ClassDescriptor>()
            .filter { it.annotations.any { it.fqName.toString() == "org.jetbrains.gradle.benchmarks.State" } }
            .forEach(process)
    }

    for (subpackageName in module.getSubPackagesOf(packageView.fqName, MemberScope.ALL_NAME_FILTER)) {
        processPackage(module, module.getPackage(subpackageName), process)
    }
}
